package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentConceptRate(int id, int studentId, int conceptId, BigDecimal amount,
                                LocalDate effectiveFrom, LocalDate effectiveUntil,
                                String reason, String authorizedBy) { }
