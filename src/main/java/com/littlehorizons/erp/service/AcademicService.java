package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Academic calculations using Stream API.
 */
public class AcademicService {

    public BigDecimal averageBySubject(List<AcademicRecord> records, int studentId, int subjectId) {
        return average(records.stream()
                .filter(record -> record.getStudentId() == studentId && record.getSubjectId() == subjectId)
                .map(AcademicRecord::getGradeValue)
                .toList());
    }

    public BigDecimal studentGeneralAverage(List<AcademicRecord> records, int studentId, int academicYear, String period) {
        return average(records.stream()
                .filter(record -> record.getStudentId() == studentId)
                .filter(record -> record.getAcademicYear() == academicYear)
                .filter(record -> period == null || period.isBlank() || period.equals(record.getAcademicPeriod()))
                .map(AcademicRecord::getGradeValue)
                .toList());
    }

    public BigDecimal courseAverageForSubject(List<AcademicRecord> records, int subjectId, int academicYear, String period) {
        return average(records.stream()
                .filter(record -> record.getSubjectId() == subjectId)
                .filter(record -> record.getAcademicYear() == academicYear)
                .filter(record -> period == null || period.isBlank() || period.equals(record.getAcademicPeriod()))
                .map(AcademicRecord::getGradeValue)
                .toList());
    }

    public Map<Integer, Long> absencesByStudent(List<AttendanceRecord> absences) {
        return absences.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public List<Map.Entry<Integer, Long>> studentsWithExcessiveAbsences(List<AttendanceRecord> absences, long threshold) {
        return absencesByStudent(absences).entrySet().stream()
                .filter(entry -> entry.getValue() > threshold)
                .toList();
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }
}
