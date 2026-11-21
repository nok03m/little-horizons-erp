package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** One academic grade together with its subject details for performance analysis. */
public record AcademicPerformanceRecord(
        int recordId,
        BigDecimal gradeValue,
        String academicPeriod,
        int subjectId,
        String subjectCode,
        String subjectName) {
}
