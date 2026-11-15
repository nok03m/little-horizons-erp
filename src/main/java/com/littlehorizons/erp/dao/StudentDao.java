package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
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

public class StudentDao {

    public List<Student> findAll() {
        String sql = """
                SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
                       correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado
                FROM estudiantes
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Student> students = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                students.add(mapStudent(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list students", exception);
            throw new IllegalStateException("Unable to list students.", exception);
        }
        return students;
    }

    public Optional<Student> findById(int id) {
        String sql = """
                SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
                       correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado
                FROM estudiantes
                WHERE id_estudiante = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapStudent(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find student by id=" + id, exception);
            throw new IllegalStateException("Unable to find student.", exception);
        }
        return Optional.empty();
    }

    public Optional<Student> findByDocument(String documentType, String documentNumber) {
        String sql = """
                SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
                       correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado
                FROM estudiantes
                WHERE tipo_documento = ? AND numero_documento = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapStudent(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find student by document", exception);
            throw new IllegalStateException("Unable to find student by document.", exception);
        }
        return Optional.empty();
    }

    public List<Student> searchByName(String term) {
        String sql = """
                SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono,
                       correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado
                FROM estudiantes
                WHERE primer_nombre LIKE ? OR primer_apellido LIKE ? OR numero_documento LIKE ?
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Student> students = new ArrayList<>();
        String pattern = "%" + term + "%";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapStudent(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to search students", exception);
            throw new IllegalStateException("Unable to search students.", exception);
        }
        return students;
    }

    public int insert(Student student) {
        String sql = """
                INSERT INTO estudiantes (
                    tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico,
                    tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindStudent(statement, student);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    student.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert student", exception);
            throw new IllegalStateException("Unable to create student. Check unique document.", exception);
        }
        throw new IllegalStateException("Student insert did not return an id.");
    }

    public boolean update(Student student) {
        String sql = """
                UPDATE estudiantes SET
                    tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?,
                    direccion = ?, telefono = ?, correo_electronico = ?, tipo_sangre = ?, alergias = ?,
                    condiciones_medicas = ?, fecha_ingreso = ?, estado = ?
                WHERE id_estudiante = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindStudent(statement, student);
            statement.setInt(17, student.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update student id=" + student.getId(), exception);
            throw new IllegalStateException("Unable to update student.", exception);
        }
    }

    public boolean retireById(int id) {
        String sql = "UPDATE estudiantes SET estado = 'Retirado' WHERE id_estudiante = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to retire student id=" + id, exception);
            throw new IllegalStateException("Unable to retire student.", exception);
        }
    }

    private void bindStudent(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getDocumentType());
        statement.setString(2, student.getDocumentNumber());
        statement.setString(3, student.getFirstName());
        setNullableString(statement, 4, student.getMiddleName());
        statement.setString(5, student.getLastName());
        setNullableString(statement, 6, student.getSecondLastName());
        statement.setDate(7, Date.valueOf(student.getBirthDate()));
        statement.setString(8, student.getGender());
        setNullableString(statement, 9, student.getAddress());
        setNullableString(statement, 10, student.getPhone());
        setNullableString(statement, 11, student.getEmail());
        setNullableString(statement, 12, student.getBloodType());
        setNullableString(statement, 13, student.getAllergies());
        setNullableString(statement, 14, student.getMedicalConditions());
        statement.setDate(15, Date.valueOf(student.getAdmissionDate()));
        statement.setString(16, student.getStatus());
    }

    private Student mapStudent(ResultSet resultSet) throws SQLException {
        Student student = new Student();
        student.setId(resultSet.getInt("id_estudiante"));
        student.setDocumentType(resultSet.getString("tipo_documento"));
        student.setDocumentNumber(resultSet.getString("numero_documento"));
        student.setFirstName(resultSet.getString("primer_nombre"));
        student.setMiddleName(resultSet.getString("segundo_nombre"));
        student.setLastName(resultSet.getString("primer_apellido"));
        student.setSecondLastName(resultSet.getString("segundo_apellido"));
        student.setBirthDate(resultSet.getDate("fecha_nacimiento").toLocalDate());
        student.setGender(resultSet.getString("genero"));
        student.setAddress(resultSet.getString("direccion"));
        student.setPhone(resultSet.getString("telefono"));
        student.setEmail(resultSet.getString("correo_electronico"));
        student.setBloodType(resultSet.getString("tipo_sangre"));
        student.setAllergies(resultSet.getString("alergias"));
        student.setMedicalConditions(resultSet.getString("condiciones_medicas"));
        student.setAdmissionDate(resultSet.getDate("fecha_ingreso").toLocalDate());
        student.setStatus(resultSet.getString("estado"));
        return student;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
