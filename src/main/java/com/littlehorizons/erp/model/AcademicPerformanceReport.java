package com.littlehorizons.erp.model;

import java.util.List;

/** Ranked subject averages and all subjects tied for the highest average. */
public record AcademicPerformanceReport(
        List<SubjectPerformance> subjects,
        List<SubjectPerformance> bestPerformers) {

    public AcademicPerformanceReport {
        subjects = List.copyOf(subjects);
        bestPerformers = List.copyOf(bestPerformers);
    }

    public boolean isEmpty() {
        return subjects.isEmpty();
    }
}
