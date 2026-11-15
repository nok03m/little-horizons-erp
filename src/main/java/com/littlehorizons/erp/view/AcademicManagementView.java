package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Console UI for subjects, enrollments, grades and attendance.
 */
public class AcademicManagementView {

    private static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    private static final List<String> GRADE_PERIODS = List.of("I", "II", "III", "IV", "Final");
    private static final List<String> ENROLLMENT_STATUSES = List.of("Activa", "Retirada", "Completada");
    private static final List<String> ATTENDANCE_STATUSES = List.of("Presente", "Ausente", "Tardanza", "Excusa Medica");

    private final ConsoleHelper console;
    private final AcademicController academicController;
    private final StudentController studentController;
    private final TeacherController teacherController;

    public AcademicManagementView(ConsoleHelper console,
                                  AcademicController academicController,
                                  StudentController studentController,
                                  TeacherController teacherController) {
        this.console = console;
        this.academicController = academicController;
        this.studentController = studentController;
        this.teacherController = teacherController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Academic Management ---");
            System.out.println("1. Subjects");
            System.out.println("2. Subject enrollments");
            System.out.println("3. Grades / academic records");
            System.out.println("4. Averages");
            System.out.println("5. Attendance");
            System.out.println("6. Absence report");
            System.out.println("7. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            switch (option) {
                case 1 -> showSubjects();
                case 2 -> showSubjectEnrollments();
                case 3 -> showGrades();
                case 4 -> showAverages();
                case 5 -> showAttendance();
                case 6 -> showAbsenceReport();
                case 7 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showSubjects() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Subjects ---");
            System.out.println("1. List all");
            System.out.println("2. List by grade");
            System.out.println("3. Create");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        academicController.listSubjects().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        academicController.listGrades().forEach(System.out::println);
                        int gradeId = console.readPositiveInt("Grade ID: ");
                        academicController.listSubjectsByGrade(gradeId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> createSubject();
                    case 4 -> updateSubject();
                    case 5 -> {
                        int id = console.readPositiveInt("Subject ID: ");
                        if (console.confirm("Delete subject?")) {
                            if (academicController.deleteSubject(id)) {
                                console.printSuccess("Subject deleted.");
                            } else {
                                console.printError("Subject not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Subjects menu failed", exception);
                console.pause();
            }
        }
    }

    private void createSubject() {
        List<Grade> grades = academicController.listGrades();
        grades.forEach(System.out::println);
        Subject subject = new Subject();
        subject.setCode(console.readRequired("Subject code: "));
        subject.setName(console.readRequired("Subject name: "));
        subject.setDescription(console.readOptional("Description (optional): "));
        subject.setGradeId(console.readPositiveInt("Grade ID: "));
        subject.setWeeklyHours(console.readIntInRange("Weekly hours: ", 1, 20));
        subject.setActive(console.confirm("Is active?"));
        int id = academicController.createSubject(subject);
        console.printSuccess("Subject created with ID " + id);
        console.pause();
    }

    private void updateSubject() {
        int id = console.readPositiveInt("Subject ID: ");
        academicController.getSubject(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            academicController.listGrades().forEach(System.out::println);
            Subject updated = new Subject();
            updated.setId(id);
            updated.setCode(console.readRequired("Subject code: "));
            updated.setName(console.readRequired("Subject name: "));
            updated.setDescription(console.readOptional("Description (optional): "));
            updated.setGradeId(console.readPositiveInt("Grade ID: "));
            updated.setWeeklyHours(console.readIntInRange("Weekly hours: ", 1, 20));
            updated.setActive(console.confirm("Is active?"));
            if (academicController.updateSubject(updated)) {
                console.printSuccess("Subject updated.");
            } else {
                console.printError("Subject was not updated.");
            }
        }, () -> console.printError("Subject not found."));
        console.pause();
    }

    private void showSubjectEnrollments() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Subject Enrollments ---");
            System.out.println("1. List all");
            System.out.println("2. List by student");
            System.out.println("3. Create");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        academicController.listSubjectEnrollments().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        academicController.listSubjectEnrollmentsByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> createSubjectEnrollment();
                    case 4 -> updateSubjectEnrollment();
                    case 5 -> {
                        int id = console.readPositiveInt("Enrollment ID: ");
                        if (console.confirm("Delete enrollment?")) {
                            if (academicController.deleteSubjectEnrollment(id)) {
                                console.printSuccess("Enrollment deleted.");
                            } else {
                                console.printError("Enrollment not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Subject enrollment menu failed", exception);
                console.pause();
            }
        }
    }

    private void createSubjectEnrollment() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return;
        }
        academicController.listSubjects().forEach(System.out::println);
        SubjectEnrollment enrollment = new SubjectEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSubjectId(console.readPositiveInt("Subject ID: "));
        enrollment.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
        enrollment.setAcademicPeriod(console.readChoice("Period:", PERIODS));
        enrollment.setSection(console.readOptional("Section (optional): "));
        enrollment.setEnrollmentDate(console.readDate("Enrollment date"));
        enrollment.setStatus(console.readChoice("Status:", ENROLLMENT_STATUSES));
        int id = academicController.createSubjectEnrollment(enrollment);
        console.printSuccess("Subject enrollment created with ID " + id);
        console.pause();
    }

    private void updateSubjectEnrollment() {
        int id = console.readPositiveInt("Enrollment ID: ");
        academicController.getSubjectEnrollment(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            existing.setSubjectId(console.readPositiveInt("Subject ID: "));
            existing.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
            existing.setAcademicPeriod(console.readChoice("Period:", PERIODS));
            existing.setSection(console.readOptional("Section (optional): "));
            existing.setEnrollmentDate(console.readDate("Enrollment date"));
            existing.setStatus(console.readChoice("Status:", ENROLLMENT_STATUSES));
            if (academicController.updateSubjectEnrollment(existing)) {
                console.printSuccess("Enrollment updated.");
            } else {
                console.printError("Enrollment was not updated.");
            }
        }, () -> console.printError("Enrollment not found."));
        console.pause();
    }

    private void showGrades() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Academic Records ---");
            System.out.println("1. List all");
            System.out.println("2. History by student");
            System.out.println("3. Register grade");
            System.out.println("4. Update grade");
            System.out.println("5. Delete grade");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        academicController.listAcademicRecords().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        academicController.listAcademicRecordsByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> createGrade();
                    case 4 -> updateGrade();
                    case 5 -> {
                        int id = console.readPositiveInt("Record ID: ");
                        if (console.confirm("Delete grade record?")) {
                            if (academicController.deleteAcademicRecord(id)) {
                                console.printSuccess("Grade deleted.");
                            } else {
                                console.printError("Record not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Grades menu failed", exception);
                console.pause();
            }
        }
    }

    private void createGrade() {
        AcademicRecord record = readGradeRecord(null);
        if (record == null) {
            return;
        }
        int id = academicController.createAcademicRecord(record);
        console.printSuccess("Grade registered with ID " + id);
        console.pause();
    }

    private void updateGrade() {
        int id = console.readPositiveInt("Record ID: ");
        academicController.getAcademicRecord(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            AcademicRecord updated = readGradeRecord(existing);
            if (updated == null) {
                return;
            }
            updated.setId(id);
            if (academicController.updateAcademicRecord(updated)) {
                console.printSuccess("Grade updated.");
            } else {
                console.printError("Grade was not updated.");
            }
        }, () -> console.printError("Record not found."));
        console.pause();
    }

    private AcademicRecord readGradeRecord(AcademicRecord current) {
        int studentId = current == null
                ? console.readPositiveInt("Student ID: ")
                : current.getStudentId();
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return null;
        }
        academicController.listSubjects().stream().limit(20).forEach(System.out::println);
        console.printInfo("Showing first subjects; enter any valid subject ID.");
        int subjectId = console.readPositiveInt("Subject ID: ");
        if (academicController.getSubject(subjectId).isEmpty()) {
            console.printError("Subject not found.");
            console.pause();
            return null;
        }
        int teacherId = console.readPositiveInt("Teacher ID: ");
        if (teacherController.getTeacher(teacherId).isEmpty()) {
            console.printError("Teacher not found.");
            console.pause();
            return null;
        }
        AcademicRecord record = new AcademicRecord();
        record.setStudentId(studentId);
        record.setSubjectId(subjectId);
        record.setTeacherId(teacherId);
        record.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
        record.setAcademicPeriod(console.readChoice("Period:", GRADE_PERIODS));
        record.setGradeValue(console.readGradeValue("Grade value"));
        record.setObservations(console.readOptional("Observations (optional): "));
        record.setRecordDate(console.readDate("Record date"));
        return record;
    }

    private void showAverages() {
        System.out.println();
        System.out.println("--- Averages ---");
        System.out.println("1. Student average by subject");
        System.out.println("2. Student general average (year/period)");
        System.out.println("3. Course average by subject");
        int option = console.readIntInRange("Select an option: ", 1, 3);
        try {
            switch (option) {
                case 1 -> {
                    int studentId = console.readPositiveInt("Student ID: ");
                    int subjectId = console.readPositiveInt("Subject ID: ");
                    BigDecimal average = academicController.studentSubjectAverage(studentId, subjectId);
                    console.printSuccess("Subject average: " + average);
                }
                case 2 -> {
                    int studentId = console.readPositiveInt("Student ID: ");
                    int year = console.readIntInRange("Academic year: ", 2000, 2100);
                    String period = console.readChoice("Period (or pick any and ignore filter next):", GRADE_PERIODS);
                    if (!console.confirm("Filter by selected period?")) {
                        period = null;
                    }
                    BigDecimal average = academicController.studentGeneralAverage(studentId, year, period);
                    console.printSuccess("General average: " + average);
                }
                case 3 -> {
                    int subjectId = console.readPositiveInt("Subject ID: ");
                    int year = console.readIntInRange("Academic year: ", 2000, 2100);
                    String period = console.readChoice("Period:", GRADE_PERIODS);
                    BigDecimal average = academicController.courseSubjectAverage(subjectId, year, period);
                    console.printSuccess("Course average: " + average);
                }
                default -> console.printError("Unexpected option.");
            }
        } catch (RuntimeException exception) {
            console.printError(exception.getMessage());
            AppLogger.error("Averages failed", exception);
        }
        console.pause();
    }

    private void showAttendance() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Attendance ---");
            System.out.println("1. List all");
            System.out.println("2. List by student");
            System.out.println("3. Register");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        academicController.listAttendance().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        academicController.listAttendanceByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> createAttendance();
                    case 4 -> updateAttendance();
                    case 5 -> {
                        int id = console.readPositiveInt("Attendance ID: ");
                        if (console.confirm("Delete attendance?")) {
                            if (academicController.deleteAttendance(id)) {
                                console.printSuccess("Attendance deleted.");
                            } else {
                                console.printError("Attendance not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Attendance menu failed", exception);
                console.pause();
            }
        }
    }

    private void createAttendance() {
        AttendanceRecord record = readAttendance(null);
        if (record == null) {
            return;
        }
        int id = academicController.createAttendance(record);
        console.printSuccess("Attendance registered with ID " + id);
        console.pause();
    }

    private void updateAttendance() {
        int id = console.readPositiveInt("Attendance ID: ");
        academicController.getAttendance(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            AttendanceRecord updated = readAttendance(existing);
            if (updated == null) {
                return;
            }
            updated.setId(id);
            if (academicController.updateAttendance(updated)) {
                console.printSuccess("Attendance updated.");
            } else {
                console.printError("Attendance was not updated.");
            }
        }, () -> console.printError("Attendance not found."));
        console.pause();
    }

    private AttendanceRecord readAttendance(AttendanceRecord current) {
        int studentId = current == null
                ? console.readPositiveInt("Student ID: ")
                : current.getStudentId();
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return null;
        }
        int subjectId = console.readPositiveInt("Subject ID: ");
        if (academicController.getSubject(subjectId).isEmpty()) {
            console.printError("Subject not found.");
            console.pause();
            return null;
        }
        AttendanceRecord record = new AttendanceRecord();
        record.setStudentId(studentId);
        record.setSubjectId(subjectId);
        record.setAttendanceDate(console.readDate("Attendance date"));
        record.setStatus(console.readChoice("Status:", ATTENDANCE_STATUSES));
        record.setObservations(console.readOptional("Observations (optional): "));
        if (console.confirm("Set recording teacher?")) {
            int teacherId = console.readPositiveInt("Teacher ID: ");
            if (teacherController.getTeacher(teacherId).isEmpty()) {
                console.printError("Teacher not found.");
                console.pause();
                return null;
            }
            record.setRecordedByTeacherId(teacherId);
        }
        return record;
    }

    private void showAbsenceReport() {
        try {
            LocalDate from = console.readDate("From date");
            LocalDate to = console.readDate("To date");
            long threshold = console.readIntInRange("Alert if absences greater than: ", 0, 100);
            List<Map.Entry<Integer, Long>> report = academicController.excessiveAbsences(from, to, threshold);
            if (report.isEmpty()) {
                console.printInfo("No students exceed the absence threshold.");
            } else {
                System.out.println("Student ID | Absences");
                report.forEach(entry -> System.out.printf("%11d | %d%n", entry.getKey(), entry.getValue()));
            }
        } catch (RuntimeException exception) {
            console.printError(exception.getMessage());
            AppLogger.error("Absence report failed", exception);
        }
        console.pause();
    }
}
