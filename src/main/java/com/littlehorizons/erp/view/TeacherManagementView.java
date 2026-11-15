package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.util.List;

/**
 * Console UI for teachers and subject assignments.
 */
public class TeacherManagementView {

    private static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");
    private static final List<String> STATUSES = List.of("Activo", "Inactivo", "Licencia");

    private final ConsoleHelper console;
    private final TeacherController teacherController;
    private final AcademicController academicController;

    public TeacherManagementView(ConsoleHelper console,
                                 TeacherController teacherController,
                                 AcademicController academicController) {
        this.console = console;
        this.teacherController = teacherController;
        this.academicController = academicController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Teacher Management ---");
            System.out.println("1. Teachers CRUD");
            System.out.println("2. Subject assignments");
            System.out.println("3. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 3);
            switch (option) {
                case 1 -> showTeacherCrud();
                case 2 -> showAssignments();
                case 3 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showTeacherCrud() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Teachers ---");
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
                    case 1 -> {
                        List<Teacher> teachers = teacherController.listTeachers();
                        teachers.forEach(System.out::println);
                        console.printInfo("Total: " + teachers.size());
                        console.pause();
                    }
                    case 2 -> {
                        String term = console.readRequired("Search: ");
                        teacherController.searchTeachers(term).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> viewTeacher();
                    case 4 -> {
                        Teacher teacher = readTeacher();
                        int id = teacherController.createTeacher(teacher);
                        console.printSuccess("Teacher created with ID " + id);
                        console.pause();
                    }
                    case 5 -> updateTeacher();
                    case 6 -> deleteTeacher();
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Teacher CRUD failed", exception);
                console.pause();
            }
        }
    }

    private void viewTeacher() {
        int id = console.readPositiveInt("Teacher ID: ");
        teacherController.getTeacher(id).ifPresentOrElse(teacher -> {
            printTeacher(teacher);
            System.out.println("Assignments:");
            List<TeacherAssignment> assignments = teacherController.listAssignmentsByTeacher(id);
            if (assignments.isEmpty()) {
                System.out.println("  (none)");
            } else {
                assignments.forEach(assignment -> System.out.println("  " + assignment));
            }
        }, () -> console.printError("Teacher not found."));
        console.pause();
    }

    private void updateTeacher() {
        int id = console.readPositiveInt("Teacher ID: ");
        teacherController.getTeacher(id).ifPresentOrElse(existing -> {
            printTeacher(existing);
            Teacher updated = readTeacher();
            updated.setId(id);
            if (teacherController.updateTeacher(updated)) {
                console.printSuccess("Teacher updated.");
            } else {
                console.printError("Teacher was not updated.");
            }
        }, () -> console.printError("Teacher not found."));
        console.pause();
    }

    private void deleteTeacher() {
        int id = console.readPositiveInt("Teacher ID: ");
        teacherController.getTeacher(id).ifPresentOrElse(teacher -> {
            printTeacher(teacher);
            if (console.confirm("Delete this teacher?")) {
                if (teacherController.deleteTeacher(id)) {
                    console.printSuccess("Teacher deleted.");
                } else {
                    console.printError("Teacher was not deleted.");
                }
            }
        }, () -> console.printError("Teacher not found."));
        console.pause();
    }

    private void showAssignments() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Teacher Assignments ---");
            System.out.println("1. List all");
            System.out.println("2. List by teacher");
            System.out.println("3. Create assignment");
            System.out.println("4. Delete assignment");
            System.out.println("5. Back");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            try {
                switch (option) {
                    case 1 -> {
                        teacherController.listAssignments().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int teacherId = console.readPositiveInt("Teacher ID: ");
                        teacherController.listAssignmentsByTeacher(teacherId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> createAssignment();
                    case 4 -> {
                        int id = console.readPositiveInt("Assignment ID: ");
                        if (console.confirm("Delete assignment?")) {
                            if (teacherController.deleteAssignment(id)) {
                                console.printSuccess("Assignment deleted.");
                            } else {
                                console.printError("Assignment not found.");
                            }
                        }
                        console.pause();
                    }
                    case 5 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Assignment menu failed", exception);
                console.pause();
            }
        }
    }

    private void createAssignment() {
        int teacherId = console.readPositiveInt("Teacher ID: ");
        if (teacherController.getTeacher(teacherId).isEmpty()) {
            console.printError("Teacher not found.");
            console.pause();
            return;
        }
        List<Subject> subjects = academicController.listSubjects();
        subjects.forEach(System.out::println);
        int subjectId = console.readPositiveInt("Subject ID: ");
        if (academicController.getSubject(subjectId).isEmpty()) {
            console.printError("Subject not found.");
            console.pause();
            return;
        }
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setSubjectId(subjectId);
        assignment.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
        assignment.setSection(console.readOptional("Section (optional, e.g. A): "));
        assignment.setDayOfWeek(console.readChoice("Class day:",
                List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado")));
        assignment.setStartTime(console.readTime("Class start time"));
        assignment.setEndTime(console.readTime("Class end time"));
        try {
            int id = teacherController.createAssignment(assignment);
            console.printSuccess("Assignment created with ID " + id);
        } catch (IllegalArgumentException exception) {
            console.printError(exception.getMessage());
        } catch (IllegalStateException exception) {
            AppLogger.error("Failed to create teacher assignment", exception);
            console.printError("Unable to create assignment. Check the database migration and assignment data.");
        }
        console.pause();
    }

    private Teacher readTeacher() {
        Teacher teacher = new Teacher();
        teacher.setDocumentType(console.readChoice("Document type:", DOCUMENT_TYPES));
        teacher.setDocumentNumber(console.readRequired("Document number: "));
        teacher.setFirstName(console.readRequired("First name: "));
        teacher.setMiddleName(console.readOptional("Middle name (optional): "));
        teacher.setLastName(console.readRequired("Last name: "));
        teacher.setSecondLastName(console.readOptional("Second last name (optional): "));
        teacher.setEmail(console.readRequired("Email: "));
        teacher.setPhone(console.readOptional("Phone (optional): "));
        teacher.setSpecialization(console.readOptional("Specialization (optional): "));
        teacher.setHireDate(console.readDate("Hire date"));
        teacher.setStatus(console.readChoice("Status:", STATUSES));
        return teacher;
    }

    private void printTeacher(Teacher teacher) {
        System.out.println("ID: " + teacher.getId());
        System.out.println("Document: " + teacher.getDocumentType() + " " + teacher.getDocumentNumber());
        System.out.println("Name: " + teacher.getFullName());
        System.out.println("Email: " + teacher.getEmail());
        System.out.println("Phone: " + (teacher.getPhone() == null ? "-" : teacher.getPhone()));
        System.out.println("Specialization: " + (teacher.getSpecialization() == null ? "-" : teacher.getSpecialization()));
        System.out.println("Hire date: " + teacher.getHireDate());
        System.out.println("Status: " + teacher.getStatus());
    }
}
