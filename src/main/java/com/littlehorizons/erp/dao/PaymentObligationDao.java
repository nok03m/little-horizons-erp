package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.PaymentObligation;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class PaymentObligationDao {

    public int insert(PaymentObligation obligation) {
        if (obligation.studentId() <= 0 || obligation.conceptId() <= 0 || obligation.periodKey() == null
                || obligation.periodKey().isBlank() || obligation.periodKey().length() > 20
                || obligation.dueDate() == null || obligation.amountDue() == null
                || obligation.amountDue().signum() <= 0 || obligation.description() == null
                || obligation.description().isBlank() || obligation.createdBy() == null || obligation.createdBy().isBlank()) {
            throw new IllegalArgumentException("Student, concept, period, due date, positive amount, description and creator are required.");
        }
        String sql = """
                INSERT INTO obligaciones_pago
                    (id_estudiante, id_concepto, periodo_clave, fecha_vencimiento, monto_debido, descripcion, creado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, obligation.studentId());
            statement.setInt(2, obligation.conceptId());
            statement.setString(3, obligation.periodKey());
            statement.setDate(4, Date.valueOf(obligation.dueDate()));
            statement.setBigDecimal(5, obligation.amountDue());
            statement.setString(6, obligation.description());
            statement.setString(7, obligation.createdBy());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new IllegalStateException("Obligation insert did not return an id.");
        } catch (SQLException exception) {
            AppLogger.error("Failed to create payment obligation", exception);
            throw new IllegalStateException("Unable to create obligation. The student/concept/period may already exist.", exception);
        }
    }

    public List<PaymentObligation> findOutstanding() {
        String sql = """
                SELECT o.id_obligacion, o.id_estudiante, o.id_concepto, o.periodo_clave,
                       o.fecha_vencimiento, o.monto_debido, o.descripcion, o.creado_por,
                       COALESCE(SUM(a.monto_aplicado), 0) AS monto_aplicado
                FROM obligaciones_pago o
                LEFT JOIN aplicaciones_pago a ON a.id_obligacion = o.id_obligacion
                GROUP BY o.id_obligacion, o.id_estudiante, o.id_concepto, o.periodo_clave,
                         o.fecha_vencimiento, o.monto_debido, o.descripcion, o.creado_por
                HAVING o.monto_debido > monto_aplicado
                ORDER BY o.fecha_vencimiento, o.id_obligacion
                """;
        List<PaymentObligation> obligations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) obligations.add(map(result));
        } catch (SQLException exception) {
            AppLogger.error("Failed to list outstanding obligations", exception);
            throw new IllegalStateException("Unable to list obligations. Apply migration 006.", exception);
        }
        return obligations;
    }

    public boolean hasAllocationsForPayment(int paymentId) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM aplicaciones_pago WHERE id_pago = ? LIMIT 1")) {
            statement.setInt(1, paymentId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException exception) {
            AppLogger.error("Failed to inspect payment allocations", exception);
            throw new IllegalStateException("Unable to inspect allocations. Apply migration 006.", exception);
        }
    }

    /** Allocates this payment to oldest outstanding obligations for its student and concept. */
    public BigDecimal allocateToOldestObligations(int paymentId) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int studentId;
                int conceptId;
                BigDecimal amountPaid;
                try (PreparedStatement payment = connection.prepareStatement(
                        "SELECT id_estudiante, id_concepto, monto_pagado FROM pagos WHERE id_pago = ? FOR UPDATE")) {
                    payment.setInt(1, paymentId);
                    try (ResultSet result = payment.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Payment not found.");
                        studentId = result.getInt("id_estudiante");
                        conceptId = result.getInt("id_concepto");
                        amountPaid = result.getBigDecimal("monto_pagado");
                    }
                }
                BigDecimal alreadyApplied;
                try (PreparedStatement sum = connection.prepareStatement(
                        "SELECT COALESCE(SUM(monto_aplicado), 0) FROM aplicaciones_pago WHERE id_pago = ?")) {
                    sum.setInt(1, paymentId);
                    try (ResultSet result = sum.executeQuery()) {
                        result.next();
                        alreadyApplied = result.getBigDecimal(1);
                    }
                }
                BigDecimal remaining = amountPaid.subtract(alreadyApplied);
                if (remaining.signum() <= 0) {
                    connection.commit();
                    return BigDecimal.ZERO;
                }
                List<ObligationBalance> candidates = new ArrayList<>();
                String lockObligations = """
                        SELECT id_obligacion, monto_debido FROM obligaciones_pago
                        WHERE id_estudiante = ? AND id_concepto = ?
                        ORDER BY fecha_vencimiento, id_obligacion FOR UPDATE
                        """;
                try (PreparedStatement obligations = connection.prepareStatement(lockObligations)) {
                    obligations.setInt(1, studentId);
                    obligations.setInt(2, conceptId);
                    try (ResultSet result = obligations.executeQuery()) {
                        while (result.next()) candidates.add(new ObligationBalance(
                                result.getInt("id_obligacion"), result.getBigDecimal("monto_debido")));
                    }
                }
                BigDecimal newlyApplied = BigDecimal.ZERO;
                for (ObligationBalance obligation : candidates) {
                    if (remaining.signum() <= 0) break;
                    BigDecimal appliedToObligation;
                    try (PreparedStatement sum = connection.prepareStatement(
                            "SELECT COALESCE(SUM(monto_aplicado), 0) FROM aplicaciones_pago WHERE id_obligacion = ?")) {
                        sum.setInt(1, obligation.id());
                        try (ResultSet result = sum.executeQuery()) {
                            result.next();
                            appliedToObligation = result.getBigDecimal(1);
                        }
                    }
                    BigDecimal balance = obligation.amountDue().subtract(appliedToObligation);
                    if (balance.signum() <= 0) continue;
                    BigDecimal allocation = remaining.min(balance);
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO aplicaciones_pago (id_pago, id_obligacion, monto_aplicado) VALUES (?, ?, ?)")) {
                        insert.setInt(1, paymentId);
                        insert.setInt(2, obligation.id());
                        insert.setBigDecimal(3, allocation);
                        insert.executeUpdate();
                    }
                    remaining = remaining.subtract(allocation);
                    newlyApplied = newlyApplied.add(allocation);
                }
                connection.commit();
                return newlyApplied;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to allocate payment id=" + paymentId, exception);
            throw new IllegalStateException("Unable to allocate payment to obligations.", exception);
        }
    }

    public List<DelinquentStudent> findDelinquentStudents(int daysThreshold) {
        if (daysThreshold < 1 || daysThreshold > 365) throw new IllegalArgumentException("Days threshold must be between 1 and 365.");
        String sql = """
                SELECT e.id_estudiante, CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       MIN(CASE WHEN o.fecha_vencimiento < DATE_SUB(CURDATE(), INTERVAL ? DAY)
                                AND o.monto_debido > COALESCE(a.monto_aplicado, 0) THEN o.fecha_vencimiento END) AS oldest_due,
                       SUM(CASE WHEN o.fecha_vencimiento < DATE_SUB(CURDATE(), INTERVAL ? DAY)
                                THEN o.monto_debido - COALESCE(a.monto_aplicado, 0) ELSE 0 END) AS outstanding_balance
                FROM obligaciones_pago o
                JOIN estudiantes e ON e.id_estudiante = o.id_estudiante
                LEFT JOIN (SELECT id_obligacion, SUM(monto_aplicado) AS monto_aplicado
                           FROM aplicaciones_pago GROUP BY id_obligacion) a ON a.id_obligacion = o.id_obligacion
                WHERE e.estado = 'Activo'
                GROUP BY e.id_estudiante, e.primer_nombre, e.primer_apellido
                HAVING outstanding_balance > 0
                ORDER BY oldest_due, e.id_estudiante
                """;
        List<DelinquentStudent> students = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, daysThreshold);
            statement.setInt(2, daysThreshold);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    DelinquentStudent delinquent = new DelinquentStudent();
                    delinquent.setStudentId(result.getInt("id_estudiante"));
                    delinquent.setStudentName(result.getString("student_name"));
                    Date oldestDue = result.getDate("oldest_due");
                    delinquent.setOldestDueDate(oldestDue.toLocalDate());
                    delinquent.setOutstandingBalance(result.getBigDecimal("outstanding_balance"));
                    delinquent.setDaysOverdue(ChronoUnit.DAYS.between(oldestDue.toLocalDate(), LocalDate.now()));
                    students.add(delinquent);
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list delinquent students", exception);
            throw new IllegalStateException("Unable to list delinquent students. Apply migration 006.", exception);
        }
        return students;
    }

    private PaymentObligation map(ResultSet result) throws SQLException {
        return new PaymentObligation(result.getInt("id_obligacion"), result.getInt("id_estudiante"),
                result.getInt("id_concepto"), result.getString("periodo_clave"),
                result.getDate("fecha_vencimiento").toLocalDate(), result.getBigDecimal("monto_debido"),
                result.getBigDecimal("monto_aplicado"), result.getString("descripcion"), result.getString("creado_por"));
    }

    private record ObligationBalance(int id, BigDecimal amountDue) { }
}
