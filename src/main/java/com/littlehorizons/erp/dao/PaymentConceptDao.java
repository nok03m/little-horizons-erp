package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
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

public class PaymentConceptDao {

    public List<PaymentConcept> findAll() {
        String sql = """
                SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo
                FROM conceptos_pago
                ORDER BY nombre_concepto
                """;
        List<PaymentConcept> concepts = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                concepts.add(mapConcept(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list payment concepts", exception);
            throw new IllegalStateException("Unable to list payment concepts.", exception);
        }
        return concepts;
    }

    public Optional<PaymentConcept> findById(int id) {
        String sql = """
                SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo
                FROM conceptos_pago
                WHERE id_concepto = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapConcept(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find payment concept id=" + id, exception);
            throw new IllegalStateException("Unable to find payment concept.", exception);
        }
        return Optional.empty();
    }

    public int insert(PaymentConcept concept) {
        String sql = """
                INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindConcept(statement, concept);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    concept.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert payment concept", exception);
            throw new IllegalStateException("Unable to create payment concept. Code may already exist.", exception);
        }
        throw new IllegalStateException("Payment concept insert did not return an id.");
    }

    public boolean update(PaymentConcept concept) {
        String sql = """
                UPDATE conceptos_pago SET
                    codigo_concepto = ?, nombre_concepto = ?, descripcion = ?, monto_base = ?,
                    es_recurrente = ?, esta_activo = ?
                WHERE id_concepto = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindConcept(statement, concept);
            statement.setInt(7, concept.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update payment concept id=" + concept.getId(), exception);
            throw new IllegalStateException("Unable to update payment concept.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM conceptos_pago WHERE id_concepto = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete payment concept id=" + id, exception);
            throw new IllegalStateException("Unable to delete payment concept.", exception);
        }
    }

    private void bindConcept(PreparedStatement statement, PaymentConcept concept) throws SQLException {
        statement.setString(1, concept.getCode());
        statement.setString(2, concept.getName());
        if (concept.getDescription() == null || concept.getDescription().isBlank()) {
            statement.setNull(3, Types.VARCHAR);
        } else {
            statement.setString(3, concept.getDescription());
        }
        statement.setBigDecimal(4, concept.getBaseAmount());
        statement.setBoolean(5, concept.isRecurrent());
        statement.setBoolean(6, concept.isActive());
    }

    private PaymentConcept mapConcept(ResultSet resultSet) throws SQLException {
        PaymentConcept concept = new PaymentConcept();
        concept.setId(resultSet.getInt("id_concepto"));
        concept.setCode(resultSet.getString("codigo_concepto"));
        concept.setName(resultSet.getString("nombre_concepto"));
        concept.setDescription(resultSet.getString("descripcion"));
        concept.setBaseAmount(resultSet.getBigDecimal("monto_base"));
        concept.setRecurrent(resultSet.getBoolean("es_recurrente"));
        concept.setActive(resultSet.getBoolean("esta_activo"));
        return concept;
    }
}
