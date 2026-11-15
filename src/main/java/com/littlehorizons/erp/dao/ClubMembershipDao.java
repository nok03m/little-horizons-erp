package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClubMembershipDao {

    public List<ClubMembership> findAll() {
        return query(baseSelect() + " ORDER BY c.nombre_club, e.primer_apellido", null, null);
    }

    public List<ClubMembership> findByClubId(int clubId) {
        return query(baseSelect() + " WHERE m.id_club = ? ORDER BY e.primer_apellido", clubId, null);
    }

    public List<ClubMembership> findByStudentId(int studentId) {
        return query(baseSelect() + " WHERE m.id_estudiante = ? ORDER BY c.nombre_club", null, studentId);
    }

    public Optional<ClubMembership> findById(int id) {
        String sql = baseSelect() + " WHERE m.id_membresia = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapMembership(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find membership id=" + id, exception);
            throw new IllegalStateException("Unable to find membership.", exception);
        }
        return Optional.empty();
    }

    public int insert(ClubMembership membership) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            return insert(connection, membership);
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert membership", exception);
            throw new IllegalStateException("Unable to create membership.", exception);
        }
    }

    public int insertWithCapacityCheck(ClubMembership membership) {
        String clubSql = "SELECT capacidad_maxima, esta_activo FROM clubes WHERE id_club = ? FOR UPDATE";
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int capacity;
                try (PreparedStatement club = connection.prepareStatement(clubSql)) {
                    club.setInt(1, membership.getClubId());
                    try (ResultSet result = club.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Club not found.");
                        capacity = result.getInt("capacidad_maxima");
                        if (!result.getBoolean("esta_activo")) {
                            throw new IllegalStateException("Cannot enroll in an inactive club.");
                        }
                    }
                }
                if ("Activa".equalsIgnoreCase(membership.getStatus())) {
                    try (PreparedStatement count = connection.prepareStatement(
                            "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'")) {
                        count.setInt(1, membership.getClubId());
                        try (ResultSet result = count.executeQuery()) {
                            if (result.next() && result.getInt(1) >= capacity) {
                                throw new IllegalStateException("Club has reached its maximum capacity (" + capacity + ").");
                            }
                        }
                    }
                }
                int id = insert(connection, membership);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to enroll membership within club capacity", exception);
            throw new IllegalStateException("Unable to create membership.", exception);
        }
    }

    private int insert(Connection connection, ClubMembership membership) throws SQLException {
        String sql = """
                INSERT INTO membresias_club (
                    id_estudiante, id_club, fecha_ingreso, estado_membresia, fecha_retiro, motivo_retiro
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMembership(statement, membership);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    membership.setId(id);
                    return id;
                }
            }
        }
        throw new IllegalStateException("Membership insert did not return an id.");
    }

    public boolean update(ClubMembership membership) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer previousClubId = null;
                try (PreparedStatement current = connection.prepareStatement(
                        "SELECT id_club FROM membresias_club WHERE id_membresia = ? FOR UPDATE")) {
                    current.setInt(1, membership.getId());
                    try (ResultSet result = current.executeQuery()) {
                        if (!result.next()) return false;
                        previousClubId = result.getInt("id_club");
                    }
                }
                int firstClubId = Math.min(previousClubId, membership.getClubId());
                int secondClubId = Math.max(previousClubId, membership.getClubId());
                int capacity = -1;
                for (int clubId : firstClubId == secondClubId ? new int[]{firstClubId} : new int[]{firstClubId, secondClubId}) {
                    try (PreparedStatement lock = connection.prepareStatement(
                            "SELECT capacidad_maxima, esta_activo FROM clubes WHERE id_club = ? FOR UPDATE")) {
                        lock.setInt(1, clubId);
                        try (ResultSet result = lock.executeQuery()) {
                            if (!result.next()) throw new IllegalArgumentException("Club not found.");
                            if (clubId == membership.getClubId()) {
                                capacity = result.getInt("capacidad_maxima");
                                if (!result.getBoolean("esta_activo")) throw new IllegalStateException("Cannot enroll in an inactive club.");
                            }
                        }
                    }
                }
                if ("Activa".equalsIgnoreCase(membership.getStatus())) {
                    try (PreparedStatement count = connection.prepareStatement(
                            "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa' AND id_membresia <> ?")) {
                        count.setInt(1, membership.getClubId());
                        count.setInt(2, membership.getId());
                        try (ResultSet result = count.executeQuery()) {
                            if (result.next() && result.getInt(1) >= capacity) {
                                throw new IllegalStateException("Club has reached its maximum capacity (" + capacity + ").");
                            }
                        }
                    }
                }
                boolean updated = update(connection, membership);
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to update membership id=" + membership.getId(), exception);
            throw new IllegalStateException("Unable to update membership.", exception);
        }
    }

    private boolean update(Connection connection, ClubMembership membership) throws SQLException {
        String sql = """
                UPDATE membresias_club SET
                    id_estudiante = ?, id_club = ?, fecha_ingreso = ?, estado_membresia = ?,
                    fecha_retiro = ?, motivo_retiro = ?
                WHERE id_membresia = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMembership(statement, membership);
            statement.setInt(7, membership.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM membresias_club WHERE id_membresia = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete membership id=" + id, exception);
            throw new IllegalStateException("Unable to delete membership.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT m.id_membresia, m.id_estudiante, m.id_club, m.fecha_ingreso, m.estado_membresia,
                       m.fecha_retiro, m.motivo_retiro,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       c.nombre_club AS club_name
                FROM membresias_club m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN clubes c ON c.id_club = m.id_club
                """;
    }

    private List<ClubMembership> query(String sql, Integer clubId, Integer studentId) {
        List<ClubMembership> memberships = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (clubId != null) {
                statement.setInt(1, clubId);
            } else if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    memberships.add(mapMembership(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list memberships", exception);
            throw new IllegalStateException("Unable to list memberships.", exception);
        }
        return memberships;
    }

    private void bindMembership(PreparedStatement statement, ClubMembership membership) throws SQLException {
        statement.setInt(1, membership.getStudentId());
        statement.setInt(2, membership.getClubId());
        statement.setDate(3, Date.valueOf(membership.getJoinDate()));
        statement.setString(4, membership.getStatus());
        if (membership.getWithdrawalDate() == null) {
            statement.setNull(5, Types.DATE);
        } else {
            statement.setDate(5, Date.valueOf(membership.getWithdrawalDate()));
        }
        if (membership.getWithdrawalReason() == null || membership.getWithdrawalReason().isBlank()) {
            statement.setNull(6, Types.VARCHAR);
        } else {
            statement.setString(6, membership.getWithdrawalReason());
        }
    }

    private ClubMembership mapMembership(ResultSet resultSet) throws SQLException {
        ClubMembership membership = new ClubMembership();
        membership.setId(resultSet.getInt("id_membresia"));
        membership.setStudentId(resultSet.getInt("id_estudiante"));
        membership.setClubId(resultSet.getInt("id_club"));
        membership.setJoinDate(resultSet.getDate("fecha_ingreso").toLocalDate());
        membership.setStatus(resultSet.getString("estado_membresia"));
        Date withdrawal = resultSet.getDate("fecha_retiro");
        membership.setWithdrawalDate(withdrawal == null ? null : withdrawal.toLocalDate());
        membership.setWithdrawalReason(resultSet.getString("motivo_retiro"));
        membership.setStudentName(resultSet.getString("student_name"));
        membership.setClubName(resultSet.getString("club_name"));
        return membership;
    }
}
