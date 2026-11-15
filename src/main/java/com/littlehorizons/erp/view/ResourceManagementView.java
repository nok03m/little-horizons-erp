package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.SchoolResource;
import com.littlehorizons.erp.model.StudentLoan;
import com.littlehorizons.erp.model.TeacherLoan;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * Console UI for inventory and loans.
 */
public class ResourceManagementView {

    private static final List<String> RESOURCE_STATUSES = List.of(
            "Disponible", "Prestado", "En Mantenimiento", "Dado de Baja");

    private final ConsoleHelper console;
    private final ResourceController resourceController;
    private final StudentController studentController;
    private final TeacherController teacherController;

    public ResourceManagementView(ConsoleHelper console,
                                  ResourceController resourceController,
                                  StudentController studentController,
                                  TeacherController teacherController) {
        this.console = console;
        this.resourceController = resourceController;
        this.studentController = studentController;
        this.teacherController = teacherController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Resource Management ---");
            System.out.println("1. Categories");
            System.out.println("2. Inventory (resources)");
            System.out.println("3. Student loans");
            System.out.println("4. Teacher loans");
            System.out.println("5. Overdue loans report");
            System.out.println("6. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            switch (option) {
                case 1 -> showCategories();
                case 2 -> showResources();
                case 3 -> showStudentLoans();
                case 4 -> showTeacherLoans();
                case 5 -> showOverdueReport();
                case 6 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showCategories() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Resource Categories ---");
            System.out.println("1. List all");
            System.out.println("2. Create");
            System.out.println("3. Back");
            int option = console.readIntInRange("Select an option: ", 1, 3);
            try {
                switch (option) {
                    case 1 -> {
                        resourceController.listCategories().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        ResourceCategory category = new ResourceCategory();
                        category.setName(console.readRequired("Category name: "));
                        category.setDescription(console.readOptional("Description (optional): "));
                        int id = resourceController.createCategory(category);
                        console.printSuccess("Category created with ID " + id);
                        console.pause();
                    }
                    case 3 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Categories menu failed", exception);
                console.pause();
            }
        }
    }

    private void showResources() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Inventory ---");
            System.out.println("1. List all");
            System.out.println("2. List available");
            System.out.println("3. Create");
            System.out.println("4. Update");
            System.out.println("5. Delete");
            System.out.println("6. Back");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            try {
                switch (option) {
                    case 1 -> {
                        resourceController.listResources().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        resourceController.listAvailableResources().forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> {
                        SchoolResource resource = readResource();
                        int id = resourceController.createResource(resource);
                        console.printSuccess("Resource created with ID " + id);
                        console.pause();
                    }
                    case 4 -> updateResource();
                    case 5 -> {
                        int id = console.readPositiveInt("Resource ID: ");
                        if (console.confirm("Delete resource?")) {
                            if (resourceController.deleteResource(id)) {
                                console.printSuccess("Resource deleted.");
                            } else {
                                console.printError("Resource not found.");
                            }
                        }
                        console.pause();
                    }
                    case 6 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Inventory menu failed", exception);
                console.pause();
            }
        }
    }

    private void updateResource() {
        int id = console.readPositiveInt("Resource ID: ");
        resourceController.getResource(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            SchoolResource updated = readResource();
            updated.setId(id);
            if (resourceController.updateResource(updated)) {
                console.printSuccess("Resource updated.");
            } else {
                console.printError("Resource was not updated.");
            }
        }, () -> console.printError("Resource not found."));
        console.pause();
    }

    private SchoolResource readResource() {
        resourceController.listCategories().forEach(System.out::println);
        SchoolResource resource = new SchoolResource();
        resource.setCode(console.readRequired("Resource code: "));
        resource.setName(console.readRequired("Resource name: "));
        int categoryId = console.readPositiveInt("Category ID: ");
        if (resourceController.getCategory(categoryId).isEmpty()) {
            throw new IllegalArgumentException("Category not found.");
        }
        resource.setCategoryId(categoryId);
        resource.setDescription(console.readOptional("Description (optional): "));
        resource.setAcquisitionDate(console.readOptionalDate("Acquisition date"));
        resource.setPhysicalLocation(console.readOptional("Physical location (optional): "));
        resource.setStatus(console.readChoice("Status:", RESOURCE_STATUSES));
        return resource;
    }

    private void showStudentLoans() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Student Loans ---");
            System.out.println("1. List all");
            System.out.println("2. Loan resource to student");
            System.out.println("3. Return resource");
            System.out.println("4. Back");
            int option = console.readIntInRange("Select an option: ", 1, 4);
            try {
                switch (option) {
                    case 1 -> {
                        resourceController.listStudentLoans().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> loanToStudent();
                    case 3 -> returnStudentLoan();
                    case 4 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Student loans menu failed", exception);
                console.pause();
            }
        }
    }

    private void loanToStudent() {
        resourceController.listAvailableResources().forEach(System.out::println);
        int resourceId = console.readPositiveInt("Resource ID: ");
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            throw new IllegalArgumentException("Student not found.");
        }
        StudentLoan loan = new StudentLoan();
        loan.setResourceId(resourceId);
        loan.setStudentId(studentId);
        loan.setLoanDate(console.readDate("Loan date"));
        loan.setExpectedReturnDate(console.readDate("Expected return date"));
        loan.setObservations(console.readOptional("Observations (optional): "));
        int id = resourceController.loanToStudent(loan);
        console.printSuccess("Student loan created with ID " + id);
        console.pause();
    }

    private void returnStudentLoan() {
        resourceController.listStudentLoans().stream()
                .filter(loan -> !"Devuelto".equals(loan.getStatus()))
                .forEach(System.out::println);
        int loanId = console.readPositiveInt("Loan ID: ");
        LocalDate returnDate = console.readOptionalDate("Actual return date");
        String observations = console.readOptional("Observations (optional): ");
        if (resourceController.returnStudentLoan(loanId, returnDate, observations)) {
            console.printSuccess("Resource returned. Inventory status set to Disponible.");
        } else {
            console.printError("Loan was not updated.");
        }
        console.pause();
    }

    private void showTeacherLoans() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Teacher Loans ---");
            System.out.println("1. List all");
            System.out.println("2. Loan resource to teacher");
            System.out.println("3. Return resource");
            System.out.println("4. Back");
            int option = console.readIntInRange("Select an option: ", 1, 4);
            try {
                switch (option) {
                    case 1 -> {
                        resourceController.listTeacherLoans().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> loanToTeacher();
                    case 3 -> returnTeacherLoan();
                    case 4 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Teacher loans menu failed", exception);
                console.pause();
            }
        }
    }

    private void loanToTeacher() {
        resourceController.listAvailableResources().forEach(System.out::println);
        int resourceId = console.readPositiveInt("Resource ID: ");
        int teacherId = console.readPositiveInt("Teacher ID: ");
        if (teacherController.getTeacher(teacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found.");
        }
        TeacherLoan loan = new TeacherLoan();
        loan.setResourceId(resourceId);
        loan.setTeacherId(teacherId);
        loan.setLoanDate(console.readDate("Loan date"));
        loan.setExpectedReturnDate(console.readDate("Expected return date"));
        loan.setObservations(console.readOptional("Observations (optional): "));
        int id = resourceController.loanToTeacher(loan);
        console.printSuccess("Teacher loan created with ID " + id);
        console.pause();
    }

    private void returnTeacherLoan() {
        resourceController.listTeacherLoans().stream()
                .filter(loan -> !"Devuelto".equals(loan.getStatus()))
                .forEach(System.out::println);
        int loanId = console.readPositiveInt("Loan ID: ");
        LocalDate returnDate = console.readOptionalDate("Actual return date");
        String observations = console.readOptional("Observations (optional): ");
        if (resourceController.returnTeacherLoan(loanId, returnDate, observations)) {
            console.printSuccess("Resource returned. Inventory status set to Disponible.");
        } else {
            console.printError("Loan was not updated.");
        }
        console.pause();
    }

    private void showOverdueReport() {
        try {
            List<String> overdue = resourceController.listOverdueLoansReport();
            if (overdue.isEmpty()) {
                console.printInfo("No overdue loans.");
            } else {
                overdue.forEach(System.out::println);
                console.printInfo("Total overdue: " + overdue.size());
            }
        } catch (RuntimeException exception) {
            console.printError(exception.getMessage());
            AppLogger.error("Overdue report failed", exception);
        }
        console.pause();
    }
}
