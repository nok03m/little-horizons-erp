package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClubDao {

    public List<Club> findAll() {
        String sql = """
                SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, c.capacidad_maxima,
                       c.horario_reunion, c.esta_activo,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS supervisor_name,
                       (SELECT COUNT(*) FROM membresias_club m
                        WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') AS active_members
                FROM clubes c
                LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
                ORDER BY c.nombre_club
                """;
        List<Club> clubs = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                clubs.add(mapClub(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list clubs", exception);
            throw new IllegalStateException("Unable to list clubs.", exception);
        }
        return clubs;
    }

    public Optional<Club> findById(int id) {
        String sql = """
                SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, c.capacidad_maxima,
                       c.horario_reunion, c.esta_activo,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS supervisor_name,
                       (SELECT COUNT(*) FROM membresias_club m
                        WHERE m.id_club = c.id_club AND m.estado_membresia = 'Activa') AS active_members
                FROM clubes c
                LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
                WHERE c.id_club = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapClub(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find club id=" + id, exception);
            throw new IllegalStateException("Unable to find club.", exception);
        }
        return Optional.empty();
    }

    public int countActiveMembers(int clubId) {
        String sql = "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clubId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to count club members", exception);
            throw new IllegalStateException("Unable to count club members.", exception);
        }
        return 0;
    }

    public int insert(Club club) {
        String sql = """
                INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindClub(statement, club);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    club.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert club", exception);
            throw new IllegalStateException("Unable to create club. Name may already exist.", exception);
        }
        throw new IllegalStateException("Club insert did not return an id.");
    }

    public boolean update(Club club) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id_club FROM clubes WHERE id_club = ? FOR UPDATE")) {
                    lock.setInt(1, club.getId());
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next()) return false;
                    }
                }
                if (club.getMaxCapacity() < countActiveMembers(connection, club.getId())) {
                    throw new IllegalArgumentException("Maximum capacity cannot be lower than current active membership.");
                }
                String sql = """
                        UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?,
                            capacidad_maxima = ?, horario_reunion = ?, esta_activo = ?
                        WHERE id_club = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindClub(statement, club);
                    statement.setInt(7, club.getId());
                    boolean updated = statement.executeUpdate() > 0;
                    connection.commit();
                    return updated;
                }
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to update club id=" + club.getId(), exception);
            throw new IllegalStateException("Unable to update club.", exception);
        }
    }

    private int countActiveMembers(Connection connection, int clubId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'")) {
            statement.setInt(1, clubId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM clubes WHERE id_club = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete club id=" + id, exception);
            throw new IllegalStateException("Unable to delete club.", exception);
        }
    }

    private void bindClub(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.getName());
        if (club.getDescription() == null || club.getDescription().isBlank()) {
            statement.setNull(2, Types.VARCHAR);
        } else {
            statement.setString(2, club.getDescription());
        }
        if (club.getSupervisorTeacherId() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, club.getSupervisorTeacherId());
        }
        statement.setInt(4, club.getMaxCapacity());
        if (club.getMeetingSchedule() == null || club.getMeetingSchedule().isBlank()) {
            statement.setNull(5, Types.VARCHAR);
        } else {
            statement.setString(5, club.getMeetingSchedule());
        }
        statement.setBoolean(6, club.isActive());
    }

    private Club mapClub(ResultSet resultSet) throws SQLException {
        Club club = new Club();
        club.setId(resultSet.getInt("id_club"));
        club.setName(resultSet.getString("nombre_club"));
        club.setDescription(resultSet.getString("descripcion"));
        int supervisorId = resultSet.getInt("id_profesor_supervisor");
        club.setSupervisorTeacherId(resultSet.wasNull() ? null : supervisorId);
        club.setMaxCapacity(resultSet.getInt("capacidad_maxima"));
        club.setMeetingSchedule(resultSet.getString("horario_reunion"));
        club.setActive(resultSet.getBoolean("esta_activo"));
        club.setSupervisorTeacherName(resultSet.getString("supervisor_name"));
        club.setActiveMembers(resultSet.getInt("active_members"));
        return club;
    }
}
