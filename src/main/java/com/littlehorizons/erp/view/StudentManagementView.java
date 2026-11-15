package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardianLink;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * Console UI for student, guardian, link and enrollment management.
 */
public class StudentManagementView {

    private static final List<String> STUDENT_DOCUMENT_TYPES = List.of("TI", "CC", "CE", "Pasaporte");
    private static final List<String> GUARDIAN_DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");
    private static final List<String> GENDERS = List.of("M", "F", "Otro");
    private static final List<String> STUDENT_STATUSES = List.of("Activo", "Graduado", "Retirado", "Suspendido");
    private static final List<String> ENROLLMENT_STATUSES = List.of("Activa", "Completada", "Retirada");

    private final ConsoleHelper console;
    private final StudentController studentController;
    private final GuardianController guardianController;
    private final EnrollmentController enrollmentController;

    public StudentManagementView(ConsoleHelper console,
                                 StudentController studentController,
                                 GuardianController guardianController,
                                 EnrollmentController enrollmentController) {
        this.console = console;
        this.studentController = studentController;
        this.guardianController = guardianController;
        this.enrollmentController = enrollmentController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Student Management ---");
            System.out.println("1. Students");
            System.out.println("2. Guardians");
            System.out.println("3. Student-Guardian links");
            System.out.println("4. Enrollments (matriculas)");
            System.out.println("5. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            switch (option) {
                case 1 -> showStudentMenu();
                case 2 -> showGuardianMenu();
                case 3 -> showLinkMenu();
                case 4 -> showEnrollmentMenu();
                case 5 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showStudentMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Students ---");
            System.out.println("1. List all");
            System.out.println("2. Search");
            System.out.println("3. View by ID");
            System.out.println("4. Create");
            System.out.println("5. Update");
            System.out.println("6. Retire student");
            System.out.println("7. Back");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> listStudents();
                    case 2 -> searchStudents();
                    case 3 -> viewStudent();
                    case 4 -> createStudent();
                    case 5 -> updateStudent();
                    case 6 -> retireStudent();
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Student menu operation failed", exception);
                console.pause();
            }
        }
    }

    private void listStudents() {
        List<Student> students = studentController.listStudents();
        if (students.isEmpty()) {
            console.printInfo("No students found.");
        } else {
            students.forEach(System.out::println);
            console.printInfo("Total: " + students.size());
        }
        console.pause();
    }

    private void searchStudents() {
        String term = console.readRequired("Search by name or document: ");
        List<Student> students = studentController.searchStudents(term);
        if (students.isEmpty()) {
            console.printInfo("No matches.");
        } else {
            students.forEach(System.out::println);
        }
        console.pause();
    }

    private void viewStudent() {
        int id = console.readPositiveInt("Student ID: ");
        studentController.getStudent(id).ifPresentOrElse(student -> {
            printStudentDetails(student);
            List<StudentGuardianLink> links = guardianController.listLinksByStudent(id);
            System.out.println("Guardians:");
            if (links.isEmpty()) {
                System.out.println("  (none)");
            } else {
                links.forEach(link -> System.out.println("  " + link));
            }
            List<Enrollment> enrollments = enrollmentController.listByStudent(id);
            System.out.println("Enrollments:");
            if (enrollments.isEmpty()) {
                System.out.println("  (none)");
            } else {
                enrollments.forEach(enrollment -> System.out.println("  " + enrollment));
            }
        }, () -> console.printError("Student not found."));
        console.pause();
    }

    private void createStudent() {
        Student student = readStudentFromConsole(null);
        int id = studentController.createStudent(student);
        console.printSuccess("Student created with ID " + id);
        console.pause();
    }

    private void updateStudent() {
        int id = console.readPositiveInt("Student ID to update: ");
        studentController.getStudent(id).ifPresentOrElse(existing -> {
            printStudentDetails(existing);
            Student updated = readStudentFromConsole(existing);
            updated.setId(id);
            if (studentController.updateStudent(updated)) {
                console.printSuccess("Student updated.");
            } else {
                console.printError("Student was not updated.");
            }
        }, () -> console.printError("Student not found."));
        console.pause();
    }

    private void retireStudent() {
        int id = console.readPositiveInt("Student ID to delete: ");
        studentController.getStudent(id).ifPresentOrElse(student -> {
            printStudentDetails(student);
            if (console.confirm("Retire this student and keep all academic and financial history?")) {
                if (studentController.retireStudent(id)) {
                    console.printSuccess("Student marked as retired; history was preserved.");
                } else {
                    console.printError("Student was not retired.");
                }
            } else {
                console.printInfo("Retirement cancelled.");
            }
        }, () -> console.printError("Student not found."));
        console.pause();
    }

    private Student readStudentFromConsole(Student current) {
        Student student = new Student();
        student.setDocumentType(console.readChoice(
                "Document type:",
                STUDENT_DOCUMENT_TYPES));
        student.setDocumentNumber(console.readRequired("Document number"
                + suffix(current == null ? null : current.getDocumentNumber()) + ": "));
        student.setFirstName(console.readRequired("First name"
                + suffix(current == null ? null : current.getFirstName()) + ": "));
        student.setMiddleName(console.readOptional("Middle name (optional)"
                + suffix(current == null ? null : current.getMiddleName()) + ": "));
        student.setLastName(console.readRequired("Last name"
                + suffix(current == null ? null : current.getLastName()) + ": "));
        student.setSecondLastName(console.readOptional("Second last name (optional)"
                + suffix(current == null ? null : current.getSecondLastName()) + ": "));
        student.setBirthDate(console.readDate("Birth date"));
        student.setGender(console.readChoice("Gender:", GENDERS));
        student.setAddress(console.readOptional("Address (optional): "));
        student.setPhone(console.readOptional("Phone (optional): "));
        student.setEmail(console.readOptional("Email (optional): "));
        student.setBloodType(console.readOptional("Blood type (optional): "));
        student.setAllergies(console.readOptional("Allergies (optional): "));
        student.setMedicalConditions(console.readOptional("Medical conditions (optional): "));
        student.setAdmissionDate(console.readDate("Admission date"));
        student.setStatus(console.readChoice("Status:", STUDENT_STATUSES));
        return student;
    }

    private void printStudentDetails(Student student) {
        System.out.println("ID: " + student.getId());
        System.out.println("Document: " + student.getDocumentType() + " " + student.getDocumentNumber());
        System.out.println("Name: " + student.getFullName());
        System.out.println("Birth date: " + student.getBirthDate());
        System.out.println("Gender: " + student.getGender());
        System.out.println("Address: " + nullToDash(student.getAddress()));
        System.out.println("Phone: " + nullToDash(student.getPhone()));
        System.out.println("Email: " + nullToDash(student.getEmail()));
        System.out.println("Blood type: " + nullToDash(student.getBloodType()));
        System.out.println("Allergies: " + nullToDash(student.getAllergies()));
        System.out.println("Medical conditions: " + nullToDash(student.getMedicalConditions()));
        System.out.println("Admission date: " + student.getAdmissionDate());
        System.out.println("Status: " + student.getStatus());
    }

    private void showGuardianMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Guardians ---");
            System.out.println("1. List all");
            System.out.println("2. Search");
            System.out.println("3. View by ID");
            System.out.println("4. Create");
            System.out.println("5. Update");
            System.out.println("6. Delete");
            System.out.println("7. Back");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> listGuardians();
                    case 2 -> searchGuardians();
                    case 3 -> viewGuardian();
                    case 4 -> createGuardian();
                    case 5 -> updateGuardian();
                    case 6 -> deleteGuardian();
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Guardian menu operation failed", exception);
                console.pause();
            }
        }
    }

    private void listGuardians() {
        List<Guardian> guardians = guardianController.listGuardians();
        if (guardians.isEmpty()) {
            console.printInfo("No guardians found.");
        } else {
            guardians.forEach(System.out::println);
            console.printInfo("Total: " + guardians.size());
        }
        console.pause();
    }

    private void searchGuardians() {
        String term = console.readRequired("Search by name or document: ");
        List<Guardian> guardians = guardianController.searchGuardians(term);
        if (guardians.isEmpty()) {
            console.printInfo("No matches.");
        } else {
            guardians.forEach(System.out::println);
        }
        console.pause();
    }

    private void viewGuardian() {
        int id = console.readPositiveInt("Guardian ID: ");
        guardianController.getGuardian(id).ifPresentOrElse(guardian -> {
            printGuardianDetails(guardian);
            List<StudentGuardianLink> links = guardianController.listLinksByGuardian(id);
            System.out.println("Linked students:");
            if (links.isEmpty()) {
                System.out.println("  (none)");
            } else {
                links.forEach(link -> System.out.println("  " + link));
            }
        }, () -> console.printError("Guardian not found."));
        console.pause();
    }

    private void createGuardian() {
        Guardian guardian = readGuardianFromConsole();
        int id = guardianController.createGuardian(guardian);
        console.printSuccess("Guardian created with ID " + id);
        console.pause();
    }

    private void updateGuardian() {
        int id = console.readPositiveInt("Guardian ID to update: ");
        guardianController.getGuardian(id).ifPresentOrElse(existing -> {
            printGuardianDetails(existing);
            Guardian updated = readGuardianFromConsole();
            updated.setId(id);
            if (guardianController.updateGuardian(updated)) {
                console.printSuccess("Guardian updated.");
            } else {
                console.printError("Guardian was not updated.");
            }
        }, () -> console.printError("Guardian not found."));
        console.pause();
    }

    private void deleteGuardian() {
        int id = console.readPositiveInt("Guardian ID to delete: ");
        guardianController.getGuardian(id).ifPresentOrElse(guardian -> {
            printGuardianDetails(guardian);
            if (console.confirm("Delete this guardian?")) {
                if (guardianController.deleteGuardian(id)) {
                    console.printSuccess("Guardian deleted.");
                } else {
                    console.printError("Guardian was not deleted.");
                }
            } else {
                console.printInfo("Delete cancelled.");
            }
        }, () -> console.printError("Guardian not found."));
        console.pause();
    }

    private Guardian readGuardianFromConsole() {
        Guardian guardian = new Guardian();
        guardian.setDocumentType(console.readChoice("Document type:", GUARDIAN_DOCUMENT_TYPES));
        guardian.setDocumentNumber(console.readRequired("Document number: "));
        guardian.setFirstName(console.readRequired("First name: "));
        guardian.setMiddleName(console.readOptional("Middle name (optional): "));
        guardian.setLastName(console.readRequired("Last name: "));
        guardian.setSecondLastName(console.readOptional("Second last name (optional): "));
        guardian.setRelationship(console.readRequired("Relationship (Padre/Madre/Tutor Legal/...): "));
        guardian.setPrimaryPhone(console.readRequired("Primary phone: "));
        guardian.setSecondaryPhone(console.readOptional("Secondary phone (optional): "));
        guardian.setEmail(console.readOptional("Email (optional): "));
        guardian.setAddress(console.readOptional("Address (optional): "));
        guardian.setOccupation(console.readOptional("Occupation (optional): "));
        guardian.setFinanciallyResponsible(console.confirm("Is financially responsible?"));
        return guardian;
    }

    private void printGuardianDetails(Guardian guardian) {
        System.out.println("ID: " + guardian.getId());
        System.out.println("Document: " + guardian.getDocumentType() + " " + guardian.getDocumentNumber());
        System.out.println("Name: " + guardian.getFullName());
        System.out.println("Relationship: " + guardian.getRelationship());
        System.out.println("Primary phone: " + guardian.getPrimaryPhone());
        System.out.println("Secondary phone: " + nullToDash(guardian.getSecondaryPhone()));
        System.out.println("Email: " + nullToDash(guardian.getEmail()));
        System.out.println("Address: " + nullToDash(guardian.getAddress()));
        System.out.println("Occupation: " + nullToDash(guardian.getOccupation()));
        System.out.println("Financially responsible: " + guardian.isFinanciallyResponsible());
    }

    private void showLinkMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Student-Guardian Links ---");
            System.out.println("1. List links by student");
            System.out.println("2. List links by guardian");
            System.out.println("3. Create link");
            System.out.println("4. Remove link");
            System.out.println("5. Back");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            try {
                switch (option) {
                    case 1 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        List<StudentGuardianLink> links = guardianController.listLinksByStudent(studentId);
                        if (links.isEmpty()) {
                            console.printInfo("No links found.");
                        } else {
                            links.forEach(System.out::println);
                        }
                        console.pause();
                    }
                    case 2 -> {
                        int guardianId = console.readPositiveInt("Guardian ID: ");
                        List<StudentGuardianLink> links = guardianController.listLinksByGuardian(guardianId);
                        if (links.isEmpty()) {
                            console.printInfo("No links found.");
                        } else {
                            links.forEach(System.out::println);
                        }
                        console.pause();
                    }
                    case 3 -> createLink();
                    case 4 -> removeLink();
                    case 5 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Link menu operation failed", exception);
                console.pause();
            }
        }
    }

    private void createLink() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return;
        }
        int guardianId = console.readPositiveInt("Guardian ID: ");
        if (guardianController.getGuardian(guardianId).isEmpty()) {
            console.printError("Guardian not found.");
            console.pause();
            return;
        }
        StudentGuardianLink link = new StudentGuardianLink();
        link.setStudentId(studentId);
        link.setGuardianId(guardianId);
        link.setPrimaryContact(console.confirm("Mark as primary contact?"));
        link.setEmergencyContact(console.confirm("Mark as emergency contact?"));
        if (guardianController.linkStudentGuardian(link)) {
            console.printSuccess("Link created.");
        } else {
            console.printError("Link was not created.");
        }
        console.pause();
    }

    private void removeLink() {
        int studentId = console.readPositiveInt("Student ID: ");
        int guardianId = console.readPositiveInt("Guardian ID: ");
        if (console.confirm("Remove this link?")) {
            if (guardianController.unlinkStudentGuardian(studentId, guardianId)) {
                console.printSuccess("Link removed.");
            } else {
                console.printError("Link not found.");
            }
        } else {
            console.printInfo("Cancelled.");
        }
        console.pause();
    }

    private void showEnrollmentMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Enrollments ---");
            System.out.println("1. List all");
            System.out.println("2. List by student");
            System.out.println("3. Create");
            System.out.println("4. Update status / details");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        List<Enrollment> enrollments = enrollmentController.listEnrollments();
                        if (enrollments.isEmpty()) {
                            console.printInfo("No enrollments found.");
                        } else {
                            enrollments.forEach(System.out::println);
                            console.printInfo("Total: " + enrollments.size());
                        }
                        console.pause();
                    }
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        List<Enrollment> enrollments = enrollmentController.listByStudent(studentId);
                        if (enrollments.isEmpty()) {
                            console.printInfo("No enrollments for this student.");
                        } else {
                            enrollments.forEach(System.out::println);
                        }
                        console.pause();
                    }
                    case 3 -> createEnrollment();
                    case 4 -> updateEnrollment();
                    case 5 -> deleteEnrollment();
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Enrollment menu operation failed", exception);
                console.pause();
            }
        }
    }

    private void createEnrollment() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return;
        }
        List<Grade> grades = enrollmentController.listGrades();
        if (grades.isEmpty()) {
            console.printError("No grades available in catalog.");
            console.pause();
            return;
        }
        System.out.println("Available grades:");
        grades.forEach(System.out::println);
        int gradeId = console.readPositiveInt("Grade ID: ");
        if (enrollmentController.getGrade(gradeId).isEmpty()) {
            console.printError("Grade not found.");
            console.pause();
            return;
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setGradeId(gradeId);
        enrollment.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
        enrollment.setStatus(console.readChoice("Enrollment status:", ENROLLMENT_STATUSES));
        enrollment.setEnrollmentDate(console.readDate("Enrollment date"));
        enrollment.setObservations(console.readOptional("Observations (optional): "));
        int id = enrollmentController.createEnrollment(enrollment);
        console.printSuccess("Enrollment created with ID " + id);
        console.pause();
    }

    private void updateEnrollment() {
        int id = console.readPositiveInt("Enrollment ID: ");
        enrollmentController.getEnrollment(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            List<Grade> grades = enrollmentController.listGrades();
            grades.forEach(System.out::println);
            existing.setGradeId(console.readPositiveInt("Grade ID: "));
            existing.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
            existing.setStatus(console.readChoice("Enrollment status:", ENROLLMENT_STATUSES));
            existing.setEnrollmentDate(console.readDate("Enrollment date"));
            LocalDate withdrawal = console.readOptionalDate("Withdrawal date");
            existing.setWithdrawalDate(withdrawal);
            existing.setObservations(console.readOptional("Observations (optional): "));
            if (enrollmentController.updateEnrollment(existing)) {
                console.printSuccess("Enrollment updated.");
            } else {
                console.printError("Enrollment was not updated.");
            }
        }, () -> console.printError("Enrollment not found."));
        console.pause();
    }

    private void deleteEnrollment() {
        int id = console.readPositiveInt("Enrollment ID to delete: ");
        enrollmentController.getEnrollment(id).ifPresentOrElse(enrollment -> {
            System.out.println(enrollment);
            if (console.confirm("Delete this enrollment?")) {
                if (enrollmentController.deleteEnrollment(id)) {
                    console.printSuccess("Enrollment deleted.");
                } else {
                    console.printError("Enrollment was not deleted.");
                }
            } else {
                console.printInfo("Cancelled.");
            }
        }, () -> console.printError("Enrollment not found."));
        console.pause();
    }

    private String suffix(String current) {
        return current == null || current.isBlank() ? "" : " [" + current + "]";
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
