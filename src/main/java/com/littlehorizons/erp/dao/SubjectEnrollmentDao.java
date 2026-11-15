package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;
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

public class SubjectEnrollmentDao {

    public List<SubjectEnrollment> findAll() {
        String sql = baseSelect() + " ORDER BY i.anio_academico DESC, e.primer_apellido";
        return query(sql, null);
    }

    public List<SubjectEnrollment> findByStudentId(int studentId) {
        String sql = baseSelect() + " WHERE i.id_estudiante = ? ORDER BY i.anio_academico DESC";
        return query(sql, studentId);
    }

    public Optional<SubjectEnrollment> findById(int id) {
        String sql = baseSelect() + " WHERE i.id_inscripcion = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEnrollment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find subject enrollment id=" + id, exception);
            throw new IllegalStateException("Unable to find subject enrollment.", exception);
        }
        return Optional.empty();
    }

    public int insert(SubjectEnrollment enrollment) {
        String sql = """
                INSERT INTO inscripciones_materia (
                    id_estudiante, id_materia, anio_academico, periodo_academico,
                    seccion, fecha_inscripcion, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, enrollment.getStudentId());
            statement.setInt(2, enrollment.getSubjectId());
            statement.setInt(3, enrollment.getAcademicYear());
            statement.setString(4, enrollment.getAcademicPeriod());
            if (enrollment.getSection() == null || enrollment.getSection().isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, enrollment.getSection());
            }
            statement.setDate(6, Date.valueOf(enrollment.getEnrollmentDate()));
            statement.setString(7, enrollment.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    enrollment.setId(generatedId);
                    return generatedId;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert subject enrollment", exception);
            throw new IllegalStateException("Unable to create subject enrollment. It may already exist.", exception);
        }
        throw new IllegalStateException("Subject enrollment insert did not return an id.");
    }

    public boolean update(SubjectEnrollment enrollment) {
        String sql = """
                UPDATE inscripciones_materia SET
                    id_estudiante = ?, id_materia = ?, anio_academico = ?, periodo_academico = ?,
                    seccion = ?, fecha_inscripcion = ?, estado = ?
                WHERE id_inscripcion = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, enrollment.getStudentId());
            statement.setInt(2, enrollment.getSubjectId());
            statement.setInt(3, enrollment.getAcademicYear());
            statement.setString(4, enrollment.getAcademicPeriod());
            if (enrollment.getSection() == null || enrollment.getSection().isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, enrollment.getSection());
            }
            statement.setDate(6, Date.valueOf(enrollment.getEnrollmentDate()));
            statement.setString(7, enrollment.getStatus());
            statement.setInt(8, enrollment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update subject enrollment id=" + enrollment.getId(), exception);
            throw new IllegalStateException("Unable to update subject enrollment.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM inscripciones_materia WHERE id_inscripcion = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete subject enrollment id=" + id, exception);
            throw new IllegalStateException("Unable to delete subject enrollment.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT i.id_inscripcion, i.id_estudiante, i.id_materia, i.anio_academico, i.periodo_academico,
                       i.seccion, i.fecha_inscripcion, i.estado,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       m.nombre_materia AS subject_name
                FROM inscripciones_materia i
                JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
                JOIN materias m ON m.id_materia = i.id_materia
                """;
    }

    private List<SubjectEnrollment> query(String sql, Integer studentId) {
        List<SubjectEnrollment> enrollments = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list subject enrollments", exception);
            throw new IllegalStateException("Unable to list subject enrollments.", exception);
        }
        return enrollments;
    }

    private SubjectEnrollment mapEnrollment(ResultSet resultSet) throws SQLException {
        SubjectEnrollment enrollment = new SubjectEnrollment();
        enrollment.setId(resultSet.getInt("id_inscripcion"));
        enrollment.setStudentId(resultSet.getInt("id_estudiante"));
        enrollment.setSubjectId(resultSet.getInt("id_materia"));
        enrollment.setAcademicYear(resultSet.getInt("anio_academico"));
        enrollment.setAcademicPeriod(resultSet.getString("periodo_academico"));
        enrollment.setSection(resultSet.getString("seccion"));
        enrollment.setEnrollmentDate(resultSet.getDate("fecha_inscripcion").toLocalDate());
        enrollment.setStatus(resultSet.getString("estado"));
        enrollment.setStudentName(resultSet.getString("student_name"));
        enrollment.setSubjectName(resultSet.getString("subject_name"));
        return enrollment;
    }
}
