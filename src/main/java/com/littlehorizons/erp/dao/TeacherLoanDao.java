package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeacherLoan;
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

public class TeacherLoanDao {

    public List<TeacherLoan> findAll() {
        return query(baseSelect() + " ORDER BY p.fecha_prestamo DESC");
    }

    public List<TeacherLoan> findActiveOrOverdue() {
        return query(baseSelect() + " WHERE p.estado_prestamo IN ('Activo', 'Vencido') ORDER BY p.fecha_devolucion_esperada");
    }

    public List<TeacherLoan> findOverdue() {
        return query(baseSelect() + """
                 WHERE p.estado_prestamo IN ('Activo', 'Vencido')
                   AND p.fecha_devolucion_esperada < CURDATE()
                ORDER BY p.fecha_devolucion_esperada
                """);
    }

    public Optional<TeacherLoan> findById(int id) {
        String sql = baseSelect() + " WHERE p.id_prestamo = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapLoan(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find teacher loan id=" + id, exception);
            throw new IllegalStateException("Unable to find teacher loan.", exception);
        }
        return Optional.empty();
    }

    public int insert(TeacherLoan loan) {
        String sql = """
                INSERT INTO prestamos_profesor (
                    id_recurso, id_profesor, fecha_prestamo, fecha_devolucion_esperada,
                    fecha_devolucion_real, estado_prestamo, observaciones
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindLoan(statement, loan);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    loan.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert teacher loan", exception);
            throw new IllegalStateException("Unable to create teacher loan.", exception);
        }
        throw new IllegalStateException("Teacher loan insert did not return an id.");
    }

    public boolean update(TeacherLoan loan) {
        String sql = """
                UPDATE prestamos_profesor SET
                    id_recurso = ?, id_profesor = ?, fecha_prestamo = ?, fecha_devolucion_esperada = ?,
                    fecha_devolucion_real = ?, estado_prestamo = ?, observaciones = ?
                WHERE id_prestamo = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindLoan(statement, loan);
            statement.setInt(8, loan.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update teacher loan id=" + loan.getId(), exception);
            throw new IllegalStateException("Unable to update teacher loan.", exception);
        }
    }

    public int markOverdue() {
        String sql = """
                UPDATE prestamos_profesor
                SET estado_prestamo = 'Vencido'
                WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < CURDATE()
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            AppLogger.error("Failed to mark overdue teacher loans", exception);
            throw new IllegalStateException("Unable to mark overdue teacher loans.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.id_prestamo, p.id_recurso, p.id_profesor, p.fecha_prestamo, p.fecha_devolucion_esperada,
                       p.fecha_devolucion_real, p.estado_prestamo, p.observaciones,
                       r.nombre_recurso AS resource_name,
                       CONCAT(t.primer_nombre, ' ', t.primer_apellido) AS teacher_name
                FROM prestamos_profesor p
                JOIN recursos r ON r.id_recurso = p.id_recurso
                JOIN profesores t ON t.id_profesor = p.id_profesor
                """;
    }

    private List<TeacherLoan> query(String sql) {
        List<TeacherLoan> loans = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loans.add(mapLoan(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list teacher loans", exception);
            throw new IllegalStateException("Unable to list teacher loans.", exception);
        }
        return loans;
    }

    private void bindLoan(PreparedStatement statement, TeacherLoan loan) throws SQLException {
        statement.setInt(1, loan.getResourceId());
        statement.setInt(2, loan.getTeacherId());
        statement.setDate(3, Date.valueOf(loan.getLoanDate()));
        statement.setDate(4, Date.valueOf(loan.getExpectedReturnDate()));
        if (loan.getActualReturnDate() == null) {
            statement.setNull(5, Types.DATE);
        } else {
            statement.setDate(5, Date.valueOf(loan.getActualReturnDate()));
        }
        statement.setString(6, loan.getStatus());
        if (loan.getObservations() == null || loan.getObservations().isBlank()) {
            statement.setNull(7, Types.VARCHAR);
        } else {
            statement.setString(7, loan.getObservations());
        }
    }

    private TeacherLoan mapLoan(ResultSet resultSet) throws SQLException {
        TeacherLoan loan = new TeacherLoan();
        loan.setId(resultSet.getInt("id_prestamo"));
        loan.setResourceId(resultSet.getInt("id_recurso"));
        loan.setTeacherId(resultSet.getInt("id_profesor"));
        loan.setLoanDate(resultSet.getDate("fecha_prestamo").toLocalDate());
        loan.setExpectedReturnDate(resultSet.getDate("fecha_devolucion_esperada").toLocalDate());
        Date actual = resultSet.getDate("fecha_devolucion_real");
        loan.setActualReturnDate(actual == null ? null : actual.toLocalDate());
        loan.setStatus(resultSet.getString("estado_prestamo"));
        loan.setObservations(resultSet.getString("observaciones"));
        loan.setResourceName(resultSet.getString("resource_name"));
        loan.setTeacherName(resultSet.getString("teacher_name"));
        return loan;
    }
}
