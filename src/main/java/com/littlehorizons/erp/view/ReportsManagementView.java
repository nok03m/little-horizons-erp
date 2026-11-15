package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ReportsController;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Console UI for TXT reports and statistics.
 */
public class ReportsManagementView {

    private final ConsoleHelper console;
    private final ReportsController reportsController;

    public ReportsManagementView(ConsoleHelper console, ReportsController reportsController) {
        this.console = console;
        this.reportsController = reportsController;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Reports and Statistics (TXT) ---");
            System.out.println("1. General system statistics");
            System.out.println("2. Student academic history / averages");
            System.out.println("3. Absence report");
            System.out.println("4. Delinquent students");
            System.out.println("5. Overdue loans");
            System.out.println("6. Clubs and memberships");
            System.out.println("7. Events and participation");
            System.out.println("8. Payments by student");
            System.out.println("9. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 9);
            try {
                switch (option) {
                    case 1 -> showPath(reportsController.exportGeneralStatistics());
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        int year = console.readIntInRange("Academic year: ", 2000, 2100);
                        showPath(reportsController.exportStudentAcademicHistory(studentId, year));
                    }
                    case 3 -> {
                        LocalDate from = console.readDate("From date");
                        LocalDate to = console.readDate("To date");
                        long threshold = console.readIntInRange("Alert if absences greater than: ", 0, 100);
                        showPath(reportsController.exportAbsenceReport(from, to, threshold));
                    }
                    case 4 -> {
                        int days = console.readIntInRange("Days without payment threshold: ", 1, 365);
                        showPath(reportsController.exportDelinquentStudents(days));
                    }
                    case 5 -> showPath(reportsController.exportOverdueLoans());
                    case 6 -> showPath(reportsController.exportClubsAndMemberships());
                    case 7 -> showPath(reportsController.exportEventsAndParticipation());
                    case 8 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        showPath(reportsController.exportPaymentsByStudent(studentId));
                    }
                    case 9 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Reports menu failed", exception);
                console.pause();
            }
        }
    }

    private void showPath(Path path) {
        console.printSuccess("Report generated.");
        console.printInfo(path.toString());
        console.pause();
    }
}
