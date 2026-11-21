package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Average academic performance for one subject, retaining precision for ranking and ties. */
public record SubjectPerformance(int subjectId, String subjectCode, String subjectName, BigDecimal average) {

    public BigDecimal displayAverage() {
        return average.setScale(1, RoundingMode.HALF_UP);
    }
}
