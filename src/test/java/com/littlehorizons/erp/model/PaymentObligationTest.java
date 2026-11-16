package com.littlehorizons.erp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentObligationTest {

    @Test
    void balanceSubtractsPartialAllocations() {
        PaymentObligation obligation = new PaymentObligation(1, 7, 3, "2026-03",
                LocalDate.of(2026, 3, 10), new BigDecimal("120000.00"),
                new BigDecimal("45000.00"), "Monthly tuition", "finance");

        assertEquals(new BigDecimal("75000.00"), obligation.balance());
    }

    @Test
    void balanceTreatsMissingAllocationsAsZero() {
        PaymentObligation obligation = new PaymentObligation(1, 7, 3, "2026-03",
                LocalDate.of(2026, 3, 10), new BigDecimal("120000.00"),
                null, "Monthly tuition", "finance");

        assertEquals(new BigDecimal("120000.00"), obligation.balance());
    }
}
