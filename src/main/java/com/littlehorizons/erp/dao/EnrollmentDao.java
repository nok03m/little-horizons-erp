package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
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

public class EnrollmentDao {

    public List<Enrollment> findAll() {
        String sql = """
                SELECT m.id_matricula, m.id_estudiante, m.id_grado, m.anio_academico, m.estado_matricula,
                       m.fecha_matricula, m.fecha_retiro, m.observaciones,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       g.nombre_grado AS grade_name
                FROM matriculas m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN grados g ON g.id_grado = m.id_grado
                ORDER BY m.anio_academico DESC, e.primer_apellido, e.primer_nombre
                """;
        return queryList(sql);
    }

    public List<Enrollment> findByStudentId(int studentId) {
        String sql = """
                SELECT m.id_matricula, m.id_estudiante, m.id_grado, m.anio_academico, m.estado_matricula,
                       m.fecha_matricula, m.fecha_retiro, m.observaciones,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       g.nombre_grado AS grade_name
                FROM matriculas m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_estudiante = ?
                ORDER BY m.anio_academico DESC
                """;
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list enrollments for student=" + studentId, exception);
            throw new IllegalStateException("Unable to list enrollments.", exception);
        }
        return enrollments;
    }

    public Optional<Enrollment> findById(int id) {
        String sql = """
                SELECT m.id_matricula, m.id_estudiante, m.id_grado, m.anio_academico, m.estado_matricula,
                       m.fecha_matricula, m.fecha_retiro, m.observaciones,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       g.nombre_grado AS grade_name
                FROM matriculas m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_matricula = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEnrollment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find enrollment id=" + id, exception);
            throw new IllegalStateException("Unable to find enrollment.", exception);
        }
        return Optional.empty();
    }

    public int insert(Enrollment enrollment) {
        String sql = """
                INSERT INTO matriculas (
                    id_estudiante, id_grado, anio_academico, estado_matricula,
                    fecha_matricula, fecha_retiro, observaciones
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, enrollment.getStudentId());
            statement.setInt(2, enrollment.getGradeId());
            statement.setInt(3, enrollment.getAcademicYear());
            statement.setString(4, enrollment.getStatus());
            statement.setDate(5, Date.valueOf(enrollment.getEnrollmentDate()));
            if (enrollment.getWithdrawalDate() == null) {
                statement.setNull(6, Types.DATE);
            } else {
                statement.setDate(6, Date.valueOf(enrollment.getWithdrawalDate()));
            }
            if (enrollment.getObservations() == null || enrollment.getObservations().isBlank()) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, enrollment.getObservations());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    enrollment.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert enrollment", exception);
            throw new IllegalStateException("Unable to create enrollment. Student may already be enrolled for that year.", exception);
        }
        throw new IllegalStateException("Enrollment insert did not return an id.");
    }

    public boolean update(Enrollment enrollment) {
        String sql = """
                UPDATE matriculas SET
                    id_estudiante = ?, id_grado = ?, anio_academico = ?, estado_matricula = ?,
                    fecha_matricula = ?, fecha_retiro = ?, observaciones = ?
                WHERE id_matricula = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollment.getStudentId());
            statement.setInt(2, enrollment.getGradeId());
            statement.setInt(3, enrollment.getAcademicYear());
            statement.setString(4, enrollment.getStatus());
            statement.setDate(5, Date.valueOf(enrollment.getEnrollmentDate()));
            if (enrollment.getWithdrawalDate() == null) {
                statement.setNull(6, Types.DATE);
            } else {
                statement.setDate(6, Date.valueOf(enrollment.getWithdrawalDate()));
            }
            if (enrollment.getObservations() == null || enrollment.getObservations().isBlank()) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, enrollment.getObservations());
            }
            statement.setInt(8, enrollment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update enrollment id=" + enrollment.getId(), exception);
            throw new IllegalStateException("Unable to update enrollment.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM matriculas WHERE id_matricula = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete enrollment id=" + id, exception);
            throw new IllegalStateException("Unable to delete enrollment.", exception);
        }
    }

    private List<Enrollment> queryList(String sql) {
        List<Enrollment> enrollments = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                enrollments.add(mapEnrollment(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list enrollments", exception);
            throw new IllegalStateException("Unable to list enrollments.", exception);
        }
        return enrollments;
    }

    private Enrollment mapEnrollment(ResultSet resultSet) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(resultSet.getInt("id_matricula"));
        enrollment.setStudentId(resultSet.getInt("id_estudiante"));
        enrollment.setGradeId(resultSet.getInt("id_grado"));
        enrollment.setAcademicYear(resultSet.getInt("anio_academico"));
        enrollment.setStatus(resultSet.getString("estado_matricula"));
        enrollment.setEnrollmentDate(resultSet.getDate("fecha_matricula").toLocalDate());
        Date withdrawal = resultSet.getDate("fecha_retiro");
        enrollment.setWithdrawalDate(withdrawal == null ? null : withdrawal.toLocalDate());
        enrollment.setObservations(resultSet.getString("observaciones"));
        enrollment.setStudentName(resultSet.getString("student_name"));
        enrollment.setGradeName(resultSet.getString("grade_name"));
        return enrollment;
    }
}
