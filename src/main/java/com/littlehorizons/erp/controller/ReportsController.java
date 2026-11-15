package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.SchoolResource;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.ReportTxtService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds and exports TXT reports using existing module controllers.
 */
public class ReportsController {

    private final StudentController studentController;
    private final TeacherController teacherController;
    private final AcademicController academicController;
    private final ExtracurricularController extracurricularController;
    private final FinancialController financialController;
    private final ResourceController resourceController;
    private final AcademicService academicService;
    private final ReportTxtService reportTxtService;

    public ReportsController(StudentController studentController,
                             TeacherController teacherController,
                             AcademicController academicController,
                             ExtracurricularController extracurricularController,
                             FinancialController financialController,
                             ResourceController resourceController,
                             AcademicService academicService,
                             ReportTxtService reportTxtService) {
        this.studentController = studentController;
        this.teacherController = teacherController;
        this.academicController = academicController;
        this.extracurricularController = extracurricularController;
        this.financialController = financialController;
        this.resourceController = resourceController;
        this.academicService = academicService;
        this.reportTxtService = reportTxtService;
    }

    public Path exportGeneralStatistics() {
        List<Student> students = studentController.listStudents();
        List<Teacher> teachers = teacherController.listTeachers();
        List<SchoolResource> resources = resourceController.listResources();
        long activeStudents = students.stream().filter(s -> "Activo".equalsIgnoreCase(s.getStatus())).count();
        long availableResources = resources.stream()
                .filter(r -> ResourceController.STATUS_AVAILABLE.equals(r.getStatus()))
                .count();

        List<String> lines = new ArrayList<>();
        lines.add("Students total: " + students.size());
        lines.add("Students active: " + activeStudents);
        lines.add("Teachers total: " + teachers.size());
        lines.add("Subjects total: " + academicController.listSubjects().size());
        lines.add("Clubs total: " + extracurricularController.listClubs().size());
        lines.add("Events total: " + extracurricularController.listEvents().size());
        lines.add("Payments total: " + financialController.listPayments().size());
        lines.add("Invoices total: " + financialController.listInvoices().size());
        lines.add("Resources total: " + resources.size());
        lines.add("Resources available: " + availableResources);
        lines.add("Student loans total: " + resourceController.listStudentLoans().size());
        lines.add("Teacher loans total: " + resourceController.listTeacherLoans().size());
        return reportTxtService.writeReport("general_stats", "General System Statistics", lines);
    }

    public Path exportStudentAcademicHistory(int studentId, int academicYear) {
        Student student = studentController.getStudent(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        List<AcademicRecord> records = academicController.listAcademicRecordsByStudent(studentId);
        BigDecimal generalAverage = academicController.studentGeneralAverage(studentId, academicYear, null);

        List<String> lines = new ArrayList<>();
        lines.add("Student: #" + student.getId() + " " + student.getFullName());
        lines.add("Document: " + student.getDocumentType() + " " + student.getDocumentNumber());
        lines.add("Status: " + student.getStatus());
        lines.add("Academic year filter: " + academicYear);
        lines.add("General average (year): " + generalAverage);
        lines.add("");
        lines.add("Records:");
        if (records.isEmpty()) {
            lines.add("  (none)");
        } else {
            records.stream()
                    .filter(r -> r.getAcademicYear() == academicYear)
                    .forEach(r -> lines.add("  " + r));
            Map<Integer, BigDecimal> bySubject = records.stream()
                    .filter(r -> r.getAcademicYear() == academicYear)
                    .collect(Collectors.groupingBy(AcademicRecord::getSubjectId))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> academicService.averageBySubject(records, studentId, e.getKey())));
            lines.add("");
            lines.add("Averages by subject:");
            bySubject.forEach((subjectId, avg) -> lines.add("  subject #" + subjectId + " => " + avg));
        }
        return reportTxtService.writeReport(
                "academic_history_student_" + studentId,
                "Student Academic History",
                lines);
    }

    public Path exportAbsenceReport(LocalDate from, LocalDate to, long threshold) {
        List<Map.Entry<Integer, Long>> excessive = academicController.excessiveAbsences(from, to, threshold);
        List<AttendanceRecord> all = academicController.listAttendance().stream()
                .filter(a -> !a.getAttendanceDate().isBefore(from) && !a.getAttendanceDate().isAfter(to))
                .filter(a -> "Ausente".equalsIgnoreCase(a.getStatus()))
                .toList();

        List<String> lines = new ArrayList<>();
        lines.add("Range: " + from + " to " + to);
        lines.add("Threshold (absences >): " + threshold);
        lines.add("Total absence records in range: " + all.size());
        lines.add("");
        lines.add("Students exceeding threshold:");
        if (excessive.isEmpty()) {
            lines.add("  (none)");
        } else {
            excessive.forEach(e -> {
                String name = studentController.getStudent(e.getKey())
                        .map(Student::getFullName)
                        .orElse("Unknown");
                lines.add("  student #" + e.getKey() + " " + name + " => " + e.getValue() + " absences");
            });
        }
        lines.add("");
        lines.add("Detail:");
        all.forEach(a -> lines.add("  " + a));
        return reportTxtService.writeReport("absences", "Absence Report", lines);
    }

    public Path exportDelinquentStudents(int daysThreshold) {
        List<DelinquentStudent> delinquents = financialController.listDelinquentStudents(daysThreshold);
        List<String> lines = new ArrayList<>();
        lines.add("Minimum days overdue: " + daysThreshold);
        lines.add("Total: " + delinquents.size());
        lines.add("");
        delinquents.forEach(d -> lines.add(d.toString()));
        return reportTxtService.writeReport("delinquents", "Delinquent Students Report", lines);
    }

    public Path exportOverdueLoans() {
        List<String> overdue = resourceController.listOverdueLoansReport();
        List<String> lines = new ArrayList<>();
        lines.add("Total overdue: " + overdue.size());
        lines.add("");
        if (overdue.isEmpty()) {
            lines.add("(none)");
        } else {
            lines.addAll(overdue);
        }
        return reportTxtService.writeReport("overdue_loans", "Overdue Loans Report", lines);
    }

    public Path exportClubsAndMemberships() {
        List<Club> clubs = extracurricularController.listClubs();
        List<ClubMembership> memberships = extracurricularController.listMemberships();
        List<String> lines = new ArrayList<>();
        lines.add("Clubs: " + clubs.size());
        clubs.forEach(c -> lines.add("  " + c));
        lines.add("");
        lines.add("Active memberships: " + memberships.stream()
                .filter(m -> "Activa".equalsIgnoreCase(m.getStatus()))
                .count());
        memberships.forEach(m -> lines.add("  " + m));
        return reportTxtService.writeReport("clubs_memberships", "Clubs and Memberships", lines);
    }

    public Path exportEventsAndParticipation() {
        List<SchoolEvent> events = extracurricularController.listEvents();
        List<EventParticipant> participants = extracurricularController.listParticipants();
        List<String> lines = new ArrayList<>();
        lines.add("Events: " + events.size());
        events.forEach(e -> lines.add("  " + e));
        lines.add("");
        lines.add("Participations: " + participants.size());
        participants.forEach(p -> lines.add("  " + p));
        return reportTxtService.writeReport("events_participation", "Events and Participation", lines);
    }

    public Path exportPaymentsByStudent(int studentId) {
        Student student = studentController.getStudent(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        List<Payment> payments = financialController.listPaymentsByStudent(studentId);
        BigDecimal total = payments.stream()
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> lines = new ArrayList<>();
        lines.add("Student: #" + student.getId() + " " + student.getFullName());
        lines.add("Payments: " + payments.size());
        lines.add("Total paid: " + total);
        lines.add("");
        payments.forEach(p -> lines.add("  " + p));
        return reportTxtService.writeReport(
                "payments_student_" + studentId,
                "Student Payment History",
                lines);
    }
}
