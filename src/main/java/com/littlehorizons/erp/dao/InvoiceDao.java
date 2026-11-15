package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
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

public class InvoiceDao {

    public List<Invoice> findAll() {
        String sql = """
                SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo
                FROM facturas
                ORDER BY id_factura DESC
                """;
        List<Invoice> invoices = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                invoices.add(mapInvoice(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list invoices", exception);
            throw new IllegalStateException("Unable to list invoices.", exception);
        }
        return invoices;
    }

    public Optional<Invoice> findByPaymentId(int paymentId) {
        String sql = """
                SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo
                FROM facturas
                WHERE id_pago = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paymentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapInvoice(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find invoice by payment id=" + paymentId, exception);
            throw new IllegalStateException("Unable to find invoice.", exception);
        }
        return Optional.empty();
    }

    public Optional<Invoice> findById(int id) {
        String sql = """
                SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo
                FROM facturas
                WHERE id_factura = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapInvoice(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find invoice id=" + id, exception);
            throw new IllegalStateException("Unable to find invoice.", exception);
        }
        return Optional.empty();
    }

    public int nextSequence() {
        String incrementSql = "UPDATE invoice_sequence "
                + "SET next_value = LAST_INSERT_ID(next_value + 1) WHERE sequence_id = 1";
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement increment = connection.prepareStatement(incrementSql)) {
                if (increment.executeUpdate() != 1) {
                    throw new IllegalStateException("Invoice sequence is missing. Apply migration 003.");
                }
            }
            try (PreparedStatement select = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                 ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to get next invoice sequence", exception);
            throw new IllegalStateException("Unable to get next invoice number.", exception);
        }
        return 1;
    }

    public int insert(Invoice invoice) {
        String sql = """
                INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, invoice.getInvoiceNumber());
            statement.setInt(2, invoice.getPaymentId());
            statement.setDate(3, Date.valueOf(invoice.getIssueDate()));
            if (invoice.getCufe() == null || invoice.getCufe().isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, invoice.getCufe());
            }
            if (invoice.getFileName() == null || invoice.getFileName().isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, invoice.getFileName());
            }
            if (invoice.getFilePath() == null || invoice.getFilePath().isBlank()) {
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(6, invoice.getFilePath());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    invoice.setId(id);
                    try (PreparedStatement markIssued = connection.prepareStatement(
                            "UPDATE pagos SET estado_factura = 'EMITIDA' WHERE id_pago = ? AND estado_factura = 'PENDIENTE'")) {
                        markIssued.setInt(1, invoice.getPaymentId());
                        if (markIssued.executeUpdate() != 1) {
                            throw new IllegalStateException("Payment is missing or already has an invoice status other than pending.");
                        }
                    }
                    connection.commit();
                    return id;
                }
            }
            }
            throw new IllegalStateException("Invoice insert did not return an id.");
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert invoice", exception);
            throw new IllegalStateException("Unable to create invoice.", exception);
        }
    }

    private Invoice mapInvoice(ResultSet resultSet) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(resultSet.getInt("id_factura"));
        invoice.setInvoiceNumber(resultSet.getString("numero_factura"));
        invoice.setPaymentId(resultSet.getInt("id_pago"));
        invoice.setIssueDate(resultSet.getDate("fecha_emision").toLocalDate());
        invoice.setCufe(resultSet.getString("cufe"));
        invoice.setFileName(resultSet.getString("nombre_archivo_pdf"));
        invoice.setFilePath(resultSet.getString("ruta_archivo"));
        return invoice;
    }
}
