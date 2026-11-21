package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.AcademicPerformanceRecord;
import com.littlehorizons.erp.model.AcademicPerformanceReport;
import com.littlehorizons.erp.model.SubjectPerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Aggregates all recorded grades by subject using the Stream API. */
public class AcademicPerformanceService {

    private static final int RANKING_SCALE = 10;

    public AcademicPerformanceReport generateReport(List<AcademicPerformanceRecord> records) {
        List<SubjectPerformance> rankedSubjects = records.stream()
                .collect(Collectors.groupingBy(AcademicPerformanceRecord::subjectId))
                .entrySet().stream()
                .map(entry -> summarize(entry.getValue()))
                .sorted(Comparator.comparing(SubjectPerformance::average).reversed()
                        .thenComparing(SubjectPerformance::subjectCode))
                .toList();

        if (rankedSubjects.isEmpty()) {
            return new AcademicPerformanceReport(List.of(), List.of());
        }

        BigDecimal highestAverage = rankedSubjects.get(0).average();
        List<SubjectPerformance> bestPerformers = rankedSubjects.stream()
                .filter(subject -> subject.average().compareTo(highestAverage) == 0)
                .toList();
        return new AcademicPerformanceReport(rankedSubjects, bestPerformers);
    }

    private SubjectPerformance summarize(List<AcademicPerformanceRecord> records) {
        AcademicPerformanceRecord subject = records.get(0);
        BigDecimal sum = records.stream()
                .map(AcademicPerformanceRecord::gradeValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(records.size()), RANKING_SCALE, RoundingMode.HALF_UP);
        return new SubjectPerformance(subject.subjectId(), subject.subjectCode(), subject.subjectName(), average);
    }
}
