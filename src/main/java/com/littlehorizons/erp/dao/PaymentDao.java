package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentDao {

    public List<Payment> findAll() {
        return query(baseSelect() + " ORDER BY p.fecha_pago DESC, p.id_pago DESC", null, null);
    }

    public List<Payment> findByStudentId(int studentId) {
        return query(baseSelect() + " WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC", studentId, null);
    }

    public List<Payment> findByGuardianId(int guardianId) {
        return query(baseSelect() + " WHERE p.id_acudiente = ? ORDER BY p.fecha_pago DESC", null, guardianId);
    }

    public Optional<Payment> findById(int id) {
        String sql = baseSelect() + " WHERE p.id_pago = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapPayment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find payment id=" + id, exception);
            throw new IllegalStateException("Unable to find payment.", exception);
        }
        return Optional.empty();
    }

    public int insert(Payment payment) {
        String sql = """
                INSERT INTO pagos (
                    id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago,
                    numero_recibo, anio_academico, periodo_academico, notas, id_tarifa
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindPayment(statement, payment);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    payment.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert payment", exception);
            throw new IllegalStateException("Unable to create payment. Receipt number may already exist.", exception);
        }
        throw new IllegalStateException("Payment insert did not return an id.");
    }

    public boolean update(Payment payment) {
        String sql = """
                UPDATE pagos SET
                    id_estudiante = ?, id_acudiente = ?, id_concepto = ?, monto_pagado = ?, fecha_pago = ?,
                    metodo_pago = ?, numero_recibo = ?, anio_academico = ?, periodo_academico = ?, notas = ?, id_tarifa = ?
                WHERE id_pago = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPayment(statement, payment);
            statement.setInt(12, payment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update payment id=" + payment.getId(), exception);
            throw new IllegalStateException("Unable to update payment.", exception);
        }
    }

    public List<Integer> findPaymentsPendingInvoice() {
        String sql = "SELECT id_pago FROM pagos WHERE estado_factura = 'PENDIENTE' ORDER BY fecha_pago, id_pago";
        List<Integer> paymentIds = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) paymentIds.add(result.getInt("id_pago"));
        } catch (SQLException exception) {
            AppLogger.error("Failed to list payments pending invoice", exception);
            throw new IllegalStateException("Unable to list pending invoices. Apply migration 006.", exception);
        }
        return paymentIds;
    }

    public void attachRateToPayment(int paymentId, int rateId) {
        String sql = """
                UPDATE pagos p
                JOIN tarifas_estudiante_concepto t ON t.id_tarifa = ?
                    AND t.id_estudiante = p.id_estudiante AND t.id_concepto = p.id_concepto
                    AND t.monto = p.monto_pagado
                SET p.id_tarifa = t.id_tarifa
                WHERE p.id_pago = ? AND p.estado_factura = 'PENDIENTE' AND p.id_tarifa IS NULL
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rateId);
            statement.setInt(2, paymentId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Payment cannot be linked to this rate; verify student, concept, amount, and invoice status.");
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to attach rate " + rateId + " to payment " + paymentId, exception);
            throw new IllegalStateException("Unable to attach the rate to the payment. Apply migration 008.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.id_pago, p.id_estudiante, p.id_acudiente, p.id_concepto, p.monto_pagado, p.fecha_pago,
                       p.metodo_pago, p.numero_recibo, p.anio_academico, p.periodo_academico, p.notas, p.id_tarifa,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       CONCAT(a.primer_nombre, ' ', a.primer_apellido) AS guardian_name,
                       c.nombre_concepto AS concept_name
                FROM pagos p
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente
                """;
    }

    private List<Payment> query(String sql, Integer studentId, Integer guardianId) {
        List<Payment> payments = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            } else if (guardianId != null) {
                statement.setInt(1, guardianId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(mapPayment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list payments", exception);
            throw new IllegalStateException("Unable to list payments.", exception);
        }
        return payments;
    }

    private void bindPayment(PreparedStatement statement, Payment payment) throws SQLException {
        statement.setInt(1, payment.getStudentId());
        if (payment.getGuardianId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, payment.getGuardianId());
        }
        statement.setInt(3, payment.getConceptId());
        statement.setBigDecimal(4, payment.getAmountPaid());
        statement.setDate(5, Date.valueOf(payment.getPaymentDate()));
        statement.setString(6, payment.getPaymentMethod());
        if (payment.getReceiptNumber() == null || payment.getReceiptNumber().isBlank()) {
            statement.setNull(7, Types.VARCHAR);
        } else {
            statement.setString(7, payment.getReceiptNumber());
        }
        if (payment.getAcademicYear() == null) {
            statement.setNull(8, Types.INTEGER);
        } else {
            statement.setInt(8, payment.getAcademicYear());
        }
        if (payment.getAcademicPeriod() == null || payment.getAcademicPeriod().isBlank()) {
            statement.setNull(9, Types.VARCHAR);
        } else {
            statement.setString(9, payment.getAcademicPeriod());
        }
        if (payment.getNotes() == null || payment.getNotes().isBlank()) {
            statement.setNull(10, Types.VARCHAR);
        } else {
            statement.setString(10, payment.getNotes());
        }
        if (payment.getRateId() == null) statement.setNull(11, Types.INTEGER);
        else statement.setInt(11, payment.getRateId());
    }

    private Payment mapPayment(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment();
        payment.setId(resultSet.getInt("id_pago"));
        payment.setStudentId(resultSet.getInt("id_estudiante"));
        int guardianId = resultSet.getInt("id_acudiente");
        payment.setGuardianId(resultSet.wasNull() ? null : guardianId);
        payment.setConceptId(resultSet.getInt("id_concepto"));
        int rateId = resultSet.getInt("id_tarifa");
        payment.setRateId(resultSet.wasNull() ? null : rateId);
        payment.setAmountPaid(resultSet.getBigDecimal("monto_pagado"));
        payment.setPaymentDate(resultSet.getDate("fecha_pago").toLocalDate());
        payment.setPaymentMethod(resultSet.getString("metodo_pago"));
        payment.setReceiptNumber(resultSet.getString("numero_recibo"));
        int year = resultSet.getInt("anio_academico");
        payment.setAcademicYear(resultSet.wasNull() ? null : year);
        payment.setAcademicPeriod(resultSet.getString("periodo_academico"));
        payment.setNotes(resultSet.getString("notas"));
        payment.setStudentName(resultSet.getString("student_name"));
        payment.setGuardianName(resultSet.getString("guardian_name"));
        payment.setConceptName(resultSet.getString("concept_name"));
        return payment;
    }
}
