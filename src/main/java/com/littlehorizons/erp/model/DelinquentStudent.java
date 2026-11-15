package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Projection used for overdue payment reports.
 */
public class DelinquentStudent {

    private int studentId;
    private String studentName;
    private LocalDate oldestDueDate;
    private BigDecimal outstandingBalance;
    private long daysOverdue;

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public LocalDate getOldestDueDate() {
        return oldestDueDate;
    }

    public void setOldestDueDate(LocalDate oldestDueDate) {
        this.oldestDueDate = oldestDueDate;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    @Override
    public String toString() {
        return String.format("student:#%d %s | oldestDue:%s | outstanding:$%s | daysOverdue:%d",
                studentId,
                studentName == null ? "" : studentName,
                oldestDueDate == null ? "-" : oldestDueDate,
                outstandingBalance == null ? "0.00" : outstandingBalance,
                daysOverdue);
    }
}
