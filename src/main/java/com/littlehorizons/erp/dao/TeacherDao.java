package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;
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

public class TeacherDao {

    public List<Teacher> findAll() {
        String sql = """
                SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion,
                       fecha_contratacion, estado
                FROM profesores
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Teacher> teachers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                teachers.add(mapTeacher(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list teachers", exception);
            throw new IllegalStateException("Unable to list teachers.", exception);
        }
        return teachers;
    }

    public Optional<Teacher> findById(int id) {
        String sql = """
                SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion,
                       fecha_contratacion, estado
                FROM profesores
                WHERE id_profesor = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapTeacher(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find teacher id=" + id, exception);
            throw new IllegalStateException("Unable to find teacher.", exception);
        }
        return Optional.empty();
    }

    public List<Teacher> search(String term) {
        String sql = """
                SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion,
                       fecha_contratacion, estado
                FROM profesores
                WHERE primer_nombre LIKE ? OR primer_apellido LIKE ? OR numero_documento LIKE ?
                   OR correo_electronico LIKE ?
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Teacher> teachers = new ArrayList<>();
        String pattern = "%" + term + "%";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    teachers.add(mapTeacher(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to search teachers", exception);
            throw new IllegalStateException("Unable to search teachers.", exception);
        }
        return teachers;
    }

    public int insert(Teacher teacher) {
        String sql = """
                INSERT INTO profesores (
                    tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTeacher(statement, teacher);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    teacher.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert teacher", exception);
            throw new IllegalStateException("Unable to create teacher. Check unique document/email.", exception);
        }
        throw new IllegalStateException("Teacher insert did not return an id.");
    }

    public boolean update(Teacher teacher) {
        String sql = """
                UPDATE profesores SET
                    tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, telefono = ?,
                    especializacion = ?, fecha_contratacion = ?, estado = ?
                WHERE id_profesor = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTeacher(statement, teacher);
            statement.setInt(12, teacher.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update teacher id=" + teacher.getId(), exception);
            throw new IllegalStateException("Unable to update teacher.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM profesores WHERE id_profesor = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete teacher id=" + id, exception);
            throw new IllegalStateException("Unable to delete teacher.", exception);
        }
    }

    private void bindTeacher(PreparedStatement statement, Teacher teacher) throws SQLException {
        statement.setString(1, teacher.getDocumentType());
        statement.setString(2, teacher.getDocumentNumber());
        statement.setString(3, teacher.getFirstName());
        setNullable(statement, 4, teacher.getMiddleName());
        statement.setString(5, teacher.getLastName());
        setNullable(statement, 6, teacher.getSecondLastName());
        statement.setString(7, teacher.getEmail());
        setNullable(statement, 8, teacher.getPhone());
        setNullable(statement, 9, teacher.getSpecialization());
        statement.setDate(10, Date.valueOf(teacher.getHireDate()));
        statement.setString(11, teacher.getStatus());
    }

    private Teacher mapTeacher(ResultSet resultSet) throws SQLException {
        Teacher teacher = new Teacher();
        teacher.setId(resultSet.getInt("id_profesor"));
        teacher.setDocumentType(resultSet.getString("tipo_documento"));
        teacher.setDocumentNumber(resultSet.getString("numero_documento"));
        teacher.setFirstName(resultSet.getString("primer_nombre"));
        teacher.setMiddleName(resultSet.getString("segundo_nombre"));
        teacher.setLastName(resultSet.getString("primer_apellido"));
        teacher.setSecondLastName(resultSet.getString("segundo_apellido"));
        teacher.setEmail(resultSet.getString("correo_electronico"));
        teacher.setPhone(resultSet.getString("telefono"));
        teacher.setSpecialization(resultSet.getString("especializacion"));
        teacher.setHireDate(resultSet.getDate("fecha_contratacion").toLocalDate());
        teacher.setStatus(resultSet.getString("estado"));
        return teacher;
    }

    private void setNullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
