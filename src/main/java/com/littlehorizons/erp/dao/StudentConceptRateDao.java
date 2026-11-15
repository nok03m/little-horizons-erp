package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentConceptRate;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public class StudentConceptRateDao {

    public Optional<StudentConceptRate> findApplicable(int studentId, int conceptId, LocalDate date) {
        String sql = """
                SELECT id_tarifa, id_estudiante, id_concepto, monto, vigente_desde, vigente_hasta, motivo, autorizado_por
                FROM tarifas_estudiante_concepto
                WHERE id_estudiante = ? AND id_concepto = ? AND vigente_desde <= ?
                  AND (vigente_hasta IS NULL OR vigente_hasta >= ?)
                ORDER BY vigente_desde DESC LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
            statement.setDate(3, Date.valueOf(date));
            statement.setDate(4, Date.valueOf(date));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find applicable student concept rate", exception);
            throw new IllegalStateException("Unable to load the student's rate. Apply migration 005.", exception);
        }
    }

    public int insert(StudentConceptRate rate) {
        if (rate.amount() == null || rate.amount().signum() <= 0 || rate.effectiveFrom() == null
                || (rate.effectiveUntil() != null && rate.effectiveUntil().isBefore(rate.effectiveFrom()))
                || rate.reason() == null || rate.reason().isBlank()
                || rate.authorizedBy() == null || rate.authorizedBy().isBlank()) {
            throw new IllegalArgumentException("A positive amount, valid date range, reason, and authorizing user are required.");
        }
        String sql = """
                INSERT INTO tarifas_estudiante_concepto
                    (id_estudiante, id_concepto, monto, vigente_desde, vigente_hasta, motivo, autorizado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id_estudiante FROM estudiantes WHERE id_estudiante = ? FOR UPDATE")) {
                    lock.setInt(1, rate.studentId());
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Student not found.");
                    }
                }
                String priorRateSql = """
                        SELECT id_tarifa, vigente_desde FROM tarifas_estudiante_concepto
                        WHERE id_estudiante = ? AND id_concepto = ? AND vigente_desde <= ?
                          AND (vigente_hasta IS NULL OR vigente_hasta >= ?)
                        FOR UPDATE
                        """;
                try (PreparedStatement prior = connection.prepareStatement(priorRateSql)) {
                    prior.setInt(1, rate.studentId());
                    prior.setInt(2, rate.conceptId());
                    prior.setDate(3, Date.valueOf(rate.effectiveFrom()));
                    prior.setDate(4, Date.valueOf(rate.effectiveFrom()));
                    try (ResultSet result = prior.executeQuery()) {
                        if (result.next()) {
                            LocalDate previousStart = result.getDate("vigente_desde").toLocalDate();
                            if (!previousStart.isBefore(rate.effectiveFrom())) {
                                throw new IllegalArgumentException("An existing rate already starts on this date; choose a later effective date.");
                            }
                            int previousId = result.getInt("id_tarifa");
                            result.close();
                            try (PreparedStatement closePrior = connection.prepareStatement(
                                    "UPDATE tarifas_estudiante_concepto SET vigente_hasta = ? WHERE id_tarifa = ?")) {
                                closePrior.setDate(1, Date.valueOf(rate.effectiveFrom().minusDays(1)));
                                closePrior.setInt(2, previousId);
                                closePrior.executeUpdate();
                            }
                        }
                    }
                }
                String overlapSql = """
                        SELECT 1 FROM tarifas_estudiante_concepto
                        WHERE id_estudiante = ? AND id_concepto = ?
                          AND (vigente_hasta IS NULL OR vigente_hasta >= ?)
                          AND (? IS NULL OR vigente_desde <= ?)
                        LIMIT 1
                        """;
                try (PreparedStatement overlap = connection.prepareStatement(overlapSql)) {
                    overlap.setInt(1, rate.studentId());
                    overlap.setInt(2, rate.conceptId());
                    overlap.setDate(3, Date.valueOf(rate.effectiveFrom()));
                    if (rate.effectiveUntil() == null) {
                        overlap.setNull(4, java.sql.Types.DATE);
                        overlap.setNull(5, java.sql.Types.DATE);
                    } else {
                        overlap.setDate(4, Date.valueOf(rate.effectiveUntil()));
                        overlap.setDate(5, Date.valueOf(rate.effectiveUntil()));
                    }
                    try (ResultSet result = overlap.executeQuery()) {
                        if (result.next()) throw new IllegalArgumentException("This rate overlaps an existing rate for the student and concept.");
                    }
                }
                int id;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, rate.studentId());
                    statement.setInt(2, rate.conceptId());
                    statement.setBigDecimal(3, rate.amount());
                    statement.setDate(4, Date.valueOf(rate.effectiveFrom()));
                    if (rate.effectiveUntil() == null) statement.setNull(5, java.sql.Types.DATE);
                    else statement.setDate(5, Date.valueOf(rate.effectiveUntil()));
                    statement.setString(6, rate.reason());
                    statement.setString(7, rate.authorizedBy());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new IllegalStateException("Rate insert did not return an id.");
                        id = keys.getInt(1);
                    }
                }
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException rollbackException) { exception.addSuppressed(rollbackException); }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to save student concept rate", exception);
            throw new IllegalStateException("Unable to save the student's rate. Check database migration and values.", exception);
        }
    }

    private StudentConceptRate map(ResultSet result) throws SQLException {
        Date until = result.getDate("vigente_hasta");
        return new StudentConceptRate(result.getInt("id_tarifa"), result.getInt("id_estudiante"),
                result.getInt("id_concepto"), result.getBigDecimal("monto"),
                result.getDate("vigente_desde").toLocalDate(), until == null ? null : until.toLocalDate(),
                result.getString("motivo"), result.getString("autorizado_por"));
    }
}
