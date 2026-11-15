package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
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

public class SubjectDao {

    public List<Subject> findAll() {
        String sql = """
                SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, m.id_grado,
                       m.horas_semanales, m.esta_activa, g.nombre_grado
                FROM materias m
                JOIN grados g ON g.id_grado = m.id_grado
                ORDER BY g.nivel_numerico, m.codigo_materia
                """;
        return queryList(sql, null);
    }

    public List<Subject> findByGradeId(int gradeId) {
        String sql = """
                SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, m.id_grado,
                       m.horas_semanales, m.esta_activa, g.nombre_grado
                FROM materias m
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_grado = ?
                ORDER BY m.codigo_materia
                """;
        return queryList(sql, gradeId);
    }

    public Optional<Subject> findById(int id) {
        String sql = """
                SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, m.id_grado,
                       m.horas_semanales, m.esta_activa, g.nombre_grado
                FROM materias m
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.id_materia = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapSubject(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find subject id=" + id, exception);
            throw new IllegalStateException("Unable to find subject.", exception);
        }
        return Optional.empty();
    }

    public int insert(Subject subject) {
        String sql = """
                INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, subject.getCode());
            statement.setString(2, subject.getName());
            if (subject.getDescription() == null || subject.getDescription().isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, subject.getDescription());
            }
            statement.setInt(4, subject.getGradeId());
            statement.setInt(5, subject.getWeeklyHours());
            statement.setBoolean(6, subject.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    subject.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert subject", exception);
            throw new IllegalStateException("Unable to create subject. Check unique code.", exception);
        }
        throw new IllegalStateException("Subject insert did not return an id.");
    }

    public boolean update(Subject subject) {
        String sql = """
                UPDATE materias SET
                    codigo_materia = ?, nombre_materia = ?, descripcion = ?, id_grado = ?,
                    horas_semanales = ?, esta_activa = ?
                WHERE id_materia = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject.getCode());
            statement.setString(2, subject.getName());
            if (subject.getDescription() == null || subject.getDescription().isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, subject.getDescription());
            }
            statement.setInt(4, subject.getGradeId());
            statement.setInt(5, subject.getWeeklyHours());
            statement.setBoolean(6, subject.isActive());
            statement.setInt(7, subject.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update subject id=" + subject.getId(), exception);
            throw new IllegalStateException("Unable to update subject.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM materias WHERE id_materia = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete subject id=" + id, exception);
            throw new IllegalStateException("Unable to delete subject.", exception);
        }
    }

    private List<Subject> queryList(String sql, Integer gradeId) {
        List<Subject> subjects = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (gradeId != null) {
                statement.setInt(1, gradeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list subjects", exception);
            throw new IllegalStateException("Unable to list subjects.", exception);
        }
        return subjects;
    }

    private Subject mapSubject(ResultSet resultSet) throws SQLException {
        Subject subject = new Subject();
        subject.setId(resultSet.getInt("id_materia"));
        subject.setCode(resultSet.getString("codigo_materia"));
        subject.setName(resultSet.getString("nombre_materia"));
        subject.setDescription(resultSet.getString("descripcion"));
        subject.setGradeId(resultSet.getInt("id_grado"));
        subject.setWeeklyHours(resultSet.getInt("horas_semanales"));
        subject.setActive(resultSet.getBoolean("esta_activa"));
        subject.setGradeName(resultSet.getString("nombre_grado"));
        return subject;
    }
}
