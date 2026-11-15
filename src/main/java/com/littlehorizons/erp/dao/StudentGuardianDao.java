package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentGuardianLink;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentGuardianDao {

    public List<StudentGuardianLink> findByStudentId(int studentId) {
        String sql = """
                SELECT ea.id_estudiante, ea.id_acudiente, ea.es_principal, ea.es_contacto_emergencia,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       CONCAT(a.primer_nombre, ' ', a.primer_apellido) AS guardian_name,
                       a.parentesco AS relationship
                FROM estudiante_acudiente ea
                JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante
                JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
                WHERE ea.id_estudiante = ?
                ORDER BY ea.es_principal DESC, a.primer_apellido
                """;
        return query(sql, studentId);
    }

    public List<StudentGuardianLink> findByGuardianId(int guardianId) {
        String sql = """
                SELECT ea.id_estudiante, ea.id_acudiente, ea.es_principal, ea.es_contacto_emergencia,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       CONCAT(a.primer_nombre, ' ', a.primer_apellido) AS guardian_name,
                       a.parentesco AS relationship
                FROM estudiante_acudiente ea
                JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante
                JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
                WHERE ea.id_acudiente = ?
                ORDER BY e.primer_apellido, e.primer_nombre
                """;
        return query(sql, guardianId);
    }

    public boolean link(StudentGuardianLink link) {
        String sql = """
                INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, link.getStudentId());
            statement.setInt(2, link.getGuardianId());
            statement.setBoolean(3, link.isPrimaryContact());
            statement.setBoolean(4, link.isEmergencyContact());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to link student-guardian", exception);
            throw new IllegalStateException("Unable to link student and guardian. Link may already exist.", exception);
        }
    }

    public boolean unlink(int studentId, int guardianId) {
        String sql = "DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to unlink student-guardian", exception);
            throw new IllegalStateException("Unable to unlink student and guardian.", exception);
        }
    }

    private List<StudentGuardianLink> query(String sql, int id) {
        List<StudentGuardianLink> links = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StudentGuardianLink link = new StudentGuardianLink();
                    link.setStudentId(resultSet.getInt("id_estudiante"));
                    link.setGuardianId(resultSet.getInt("id_acudiente"));
                    link.setPrimaryContact(resultSet.getBoolean("es_principal"));
                    link.setEmergencyContact(resultSet.getBoolean("es_contacto_emergencia"));
                    link.setStudentName(resultSet.getString("student_name"));
                    link.setGuardianName(resultSet.getString("guardian_name"));
                    link.setRelationship(resultSet.getString("relationship"));
                    links.add(link);
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to query student-guardian links", exception);
            throw new IllegalStateException("Unable to query student-guardian links.", exception);
        }
        return links;
    }
}
