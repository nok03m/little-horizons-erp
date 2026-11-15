package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentLoan;
import com.littlehorizons.erp.model.TeacherLoan;
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

/** Keeps a resource status change and its loan record in one database transaction. */
public final class ResourceLoanTransactionDao {

    private static final String AVAILABLE = "Disponible";
    private static final String LOANED = "Prestado";
    private static final String ACTIVE = "Activo";
    private static final String RETURNED = "Devuelto";

    public int loanToStudent(StudentLoan loan) {
        return createLoan(
                loan.getResourceId(),
                "INSERT INTO prestamos_estudiante "
                        + "(id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada, "
                        + "fecha_devolucion_real, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(2, loan.getStudentId());
                    statement.setDate(3, Date.valueOf(loan.getLoanDate()));
                    statement.setDate(4, Date.valueOf(loan.getExpectedReturnDate()));
                    setNullableDate(statement, 5, loan.getActualReturnDate());
                    statement.setString(6, ACTIVE);
                    setNullableString(statement, 7, loan.getObservations());
                },
                "student");
    }

    public int loanToTeacher(TeacherLoan loan) {
        return createLoan(
                loan.getResourceId(),
                "INSERT INTO prestamos_profesor "
                        + "(id_recurso, id_profesor, fecha_prestamo, fecha_devolucion_esperada, "
                        + "fecha_devolucion_real, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(2, loan.getTeacherId());
                    statement.setDate(3, Date.valueOf(loan.getLoanDate()));
                    statement.setDate(4, Date.valueOf(loan.getExpectedReturnDate()));
                    setNullableDate(statement, 5, loan.getActualReturnDate());
                    statement.setString(6, ACTIVE);
                    setNullableString(statement, 7, loan.getObservations());
                },
                "teacher");
    }

    public boolean returnStudentLoan(int loanId, LocalDate returnDate, String observations) {
        return returnLoan("prestamos_estudiante", loanId, returnDate, observations, "student");
    }

    public boolean returnTeacherLoan(int loanId, LocalDate returnDate, String observations) {
        return returnLoan("prestamos_profesor", loanId, returnDate, observations, "teacher");
    }

    private int createLoan(int resourceId, String insertSql, LoanBinder loanBinder, String ownerType) {
        String reserveSql = "UPDATE recursos SET estado_recurso = ? "
                + "WHERE id_recurso = ? AND estado_recurso = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement reserve = connection.prepareStatement(reserveSql)) {
                reserve.setString(1, LOANED);
                reserve.setInt(2, resourceId);
                reserve.setString(3, AVAILABLE);
                if (reserve.executeUpdate() != 1) {
                    connection.rollback();
                    throw new IllegalStateException("Resource is no longer available.");
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, resourceId);
                loanBinder.bind(insert);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        connection.commit();
                        return generatedId;
                    }
                }
            }
            connection.rollback();
            throw new IllegalStateException("Loan insert did not return an id.");
        } catch (SQLException exception) {
            AppLogger.error("Failed to create " + ownerType + " resource loan", exception);
            throw new IllegalStateException("Unable to create resource loan.", exception);
        }
    }

    private boolean returnLoan(String loanTable,
                               int loanId,
                               LocalDate returnDate,
                               String observations,
                               String ownerType) {
        String findResourceSql = "SELECT id_recurso FROM " + loanTable + " WHERE id_prestamo = ? FOR UPDATE";
        String updateLoanSql = "UPDATE " + loanTable + " SET fecha_devolucion_real = ?, "
                + "estado_prestamo = ?, observaciones = COALESCE(?, observaciones) "
                + "WHERE id_prestamo = ? AND estado_prestamo IN ('Activo', 'Vencido')";
        String releaseResourceSql = "UPDATE recursos SET estado_recurso = ? "
                + "WHERE id_recurso = ? AND estado_recurso = ?";

        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            int resourceId;
            try (PreparedStatement findResource = connection.prepareStatement(findResourceSql)) {
                findResource.setInt(1, loanId);
                try (ResultSet result = findResource.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        throw new IllegalArgumentException(ownerType + " loan not found.");
                    }
                    resourceId = result.getInt("id_recurso");
                }
            }

            try (PreparedStatement updateLoan = connection.prepareStatement(updateLoanSql)) {
                updateLoan.setDate(1, Date.valueOf(returnDate == null ? LocalDate.now() : returnDate));
                updateLoan.setString(2, RETURNED);
                setNullableString(updateLoan, 3, observations);
                updateLoan.setInt(4, loanId);
                if (updateLoan.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            try (PreparedStatement releaseResource = connection.prepareStatement(releaseResourceSql)) {
                releaseResource.setString(1, AVAILABLE);
                releaseResource.setInt(2, resourceId);
                releaseResource.setString(3, LOANED);
                if (releaseResource.executeUpdate() != 1) {
                    connection.rollback();
                    throw new IllegalStateException("Resource status is inconsistent; return was rolled back.");
                }
            }

            connection.commit();
            return true;
        } catch (SQLException exception) {
            AppLogger.error("Failed to return " + ownerType + " resource loan id=" + loanId, exception);
            throw new IllegalStateException("Unable to return resource loan.", exception);
        }
    }

    private static void setNullableDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    @FunctionalInterface
    private interface LoanBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
