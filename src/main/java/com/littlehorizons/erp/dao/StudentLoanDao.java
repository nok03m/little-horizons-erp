package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentLoan;
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

public class StudentLoanDao {

    public List<StudentLoan> findAll() {
        return query(baseSelect() + " ORDER BY p.fecha_prestamo DESC", null);
    }

    public List<StudentLoan> findActiveOrOverdue() {
        return query(baseSelect() + " WHERE p.estado_prestamo IN ('Activo', 'Vencido') ORDER BY p.fecha_devolucion_esperada", null);
    }

    public List<StudentLoan> findOverdue() {
        return query(baseSelect() + """
                 WHERE p.estado_prestamo IN ('Activo', 'Vencido')
                   AND p.fecha_devolucion_esperada < CURDATE()
                ORDER BY p.fecha_devolucion_esperada
                """, null);
    }

    public Optional<StudentLoan> findById(int id) {
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
            AppLogger.error("Failed to find student loan id=" + id, exception);
            throw new IllegalStateException("Unable to find student loan.", exception);
        }
        return Optional.empty();
    }

    public int insert(StudentLoan loan) {
        String sql = """
                INSERT INTO prestamos_estudiante (
                    id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada,
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
            AppLogger.error("Failed to insert student loan", exception);
            throw new IllegalStateException("Unable to create student loan.", exception);
        }
        throw new IllegalStateException("Student loan insert did not return an id.");
    }

    public boolean update(StudentLoan loan) {
        String sql = """
                UPDATE prestamos_estudiante SET
                    id_recurso = ?, id_estudiante = ?, fecha_prestamo = ?, fecha_devolucion_esperada = ?,
                    fecha_devolucion_real = ?, estado_prestamo = ?, observaciones = ?
                WHERE id_prestamo = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindLoan(statement, loan);
            statement.setInt(8, loan.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update student loan id=" + loan.getId(), exception);
            throw new IllegalStateException("Unable to update student loan.", exception);
        }
    }

    public int markOverdue() {
        String sql = """
                UPDATE prestamos_estudiante
                SET estado_prestamo = 'Vencido'
                WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < CURDATE()
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            AppLogger.error("Failed to mark overdue student loans", exception);
            throw new IllegalStateException("Unable to mark overdue student loans.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.id_prestamo, p.id_recurso, p.id_estudiante, p.fecha_prestamo, p.fecha_devolucion_esperada,
                       p.fecha_devolucion_real, p.estado_prestamo, p.observaciones,
                       r.nombre_recurso AS resource_name,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name
                FROM prestamos_estudiante p
                JOIN recursos r ON r.id_recurso = p.id_recurso
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                """;
    }

    private List<StudentLoan> query(String sql, Integer unused) {
        List<StudentLoan> loans = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loans.add(mapLoan(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list student loans", exception);
            throw new IllegalStateException("Unable to list student loans.", exception);
        }
        return loans;
    }

    private void bindLoan(PreparedStatement statement, StudentLoan loan) throws SQLException {
        statement.setInt(1, loan.getResourceId());
        statement.setInt(2, loan.getStudentId());
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

    private StudentLoan mapLoan(ResultSet resultSet) throws SQLException {
        StudentLoan loan = new StudentLoan();
        loan.setId(resultSet.getInt("id_prestamo"));
        loan.setResourceId(resultSet.getInt("id_recurso"));
        loan.setStudentId(resultSet.getInt("id_estudiante"));
        loan.setLoanDate(resultSet.getDate("fecha_prestamo").toLocalDate());
        loan.setExpectedReturnDate(resultSet.getDate("fecha_devolucion_esperada").toLocalDate());
        Date actual = resultSet.getDate("fecha_devolucion_real");
        loan.setActualReturnDate(actual == null ? null : actual.toLocalDate());
        loan.setStatus(resultSet.getString("estado_prestamo"));
        loan.setObservations(resultSet.getString("observaciones"));
        loan.setResourceName(resultSet.getString("resource_name"));
        loan.setStudentName(resultSet.getString("student_name"));
        return loan;
    }
}
