package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
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

public class GuardianDao {

    public List<Guardian> findAll() {
        String sql = """
                SELECT id_acudiente, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario,
                       correo_electronico, direccion, ocupacion, es_responsable_financiero
                FROM acudientes
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Guardian> guardians = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                guardians.add(mapGuardian(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list guardians", exception);
            throw new IllegalStateException("Unable to list guardians.", exception);
        }
        return guardians;
    }

    public Optional<Guardian> findById(int id) {
        String sql = """
                SELECT id_acudiente, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario,
                       correo_electronico, direccion, ocupacion, es_responsable_financiero
                FROM acudientes
                WHERE id_acudiente = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapGuardian(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find guardian id=" + id, exception);
            throw new IllegalStateException("Unable to find guardian.", exception);
        }
        return Optional.empty();
    }

    public List<Guardian> searchByNameOrDocument(String term) {
        String sql = """
                SELECT id_acudiente, tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                       primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario,
                       correo_electronico, direccion, ocupacion, es_responsable_financiero
                FROM acudientes
                WHERE primer_nombre LIKE ? OR primer_apellido LIKE ? OR numero_documento LIKE ?
                ORDER BY primer_apellido, primer_nombre
                """;
        List<Guardian> guardians = new ArrayList<>();
        String pattern = "%" + term + "%";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    guardians.add(mapGuardian(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to search guardians", exception);
            throw new IllegalStateException("Unable to search guardians.", exception);
        }
        return guardians;
    }

    public int insert(Guardian guardian) {
        String sql = """
                INSERT INTO acudientes (
                    tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                    segundo_apellido, parentesco, telefono_principal, telefono_secundario,
                    correo_electronico, direccion, ocupacion, es_responsable_financiero
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindGuardian(statement, guardian);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    guardian.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert guardian", exception);
            throw new IllegalStateException("Unable to create guardian. Check unique document.", exception);
        }
        throw new IllegalStateException("Guardian insert did not return an id.");
    }

    public boolean update(Guardian guardian) {
        String sql = """
                UPDATE acudientes SET
                    tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                    primer_apellido = ?, segundo_apellido = ?, parentesco = ?, telefono_principal = ?,
                    telefono_secundario = ?, correo_electronico = ?, direccion = ?, ocupacion = ?,
                    es_responsable_financiero = ?
                WHERE id_acudiente = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindGuardian(statement, guardian);
            statement.setInt(14, guardian.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update guardian id=" + guardian.getId(), exception);
            throw new IllegalStateException("Unable to update guardian.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM acudientes WHERE id_acudiente = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete guardian id=" + id, exception);
            throw new IllegalStateException("Unable to delete guardian.", exception);
        }
    }

    private void bindGuardian(PreparedStatement statement, Guardian guardian) throws SQLException {
        statement.setString(1, guardian.getDocumentType());
        statement.setString(2, guardian.getDocumentNumber());
        statement.setString(3, guardian.getFirstName());
        setNullableString(statement, 4, guardian.getMiddleName());
        statement.setString(5, guardian.getLastName());
        setNullableString(statement, 6, guardian.getSecondLastName());
        statement.setString(7, guardian.getRelationship());
        statement.setString(8, guardian.getPrimaryPhone());
        setNullableString(statement, 9, guardian.getSecondaryPhone());
        setNullableString(statement, 10, guardian.getEmail());
        setNullableString(statement, 11, guardian.getAddress());
        setNullableString(statement, 12, guardian.getOccupation());
        statement.setBoolean(13, guardian.isFinanciallyResponsible());
    }

    private Guardian mapGuardian(ResultSet resultSet) throws SQLException {
        Guardian guardian = new Guardian();
        guardian.setId(resultSet.getInt("id_acudiente"));
        guardian.setDocumentType(resultSet.getString("tipo_documento"));
        guardian.setDocumentNumber(resultSet.getString("numero_documento"));
        guardian.setFirstName(resultSet.getString("primer_nombre"));
        guardian.setMiddleName(resultSet.getString("segundo_nombre"));
        guardian.setLastName(resultSet.getString("primer_apellido"));
        guardian.setSecondLastName(resultSet.getString("segundo_apellido"));
        guardian.setRelationship(resultSet.getString("parentesco"));
        guardian.setPrimaryPhone(resultSet.getString("telefono_principal"));
        guardian.setSecondaryPhone(resultSet.getString("telefono_secundario"));
        guardian.setEmail(resultSet.getString("correo_electronico"));
        guardian.setAddress(resultSet.getString("direccion"));
        guardian.setOccupation(resultSet.getString("ocupacion"));
        guardian.setFinanciallyResponsible(resultSet.getBoolean("es_responsable_financiero"));
        return guardian;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
