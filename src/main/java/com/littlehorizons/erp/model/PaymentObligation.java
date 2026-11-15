package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentObligation(int id, int studentId, int conceptId, String periodKey,
                                LocalDate dueDate, BigDecimal amountDue, BigDecimal amountApplied,
                                String description, String createdBy) {
    public BigDecimal balance() {
        return amountDue.subtract(amountApplied == null ? BigDecimal.ZERO : amountApplied);
    }

    @Override
    public String toString() {
        return String.format("obligation:#%d | student:#%d | concept:#%d | period:%s | due:%s | due:$%s | applied:$%s | balance:$%s",
                id, studentId, conceptId, periodKey, dueDate, amountDue,
                amountApplied == null ? BigDecimal.ZERO : amountApplied, balance());
    }
}
