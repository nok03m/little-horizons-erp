package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleHelper;
import com.littlehorizons.erp.model.ApplicationRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Main console menu for the Little Horizons ERP.
 */
public class MainMenuView {

    private final ConsoleHelper console;
    private final StudentManagementView studentManagementView;
    private final AcademicManagementView academicManagementView;
    private final TeacherManagementView teacherManagementView;
    private final ExtracurricularManagementView extracurricularManagementView;
    private final FinancialManagementView financialManagementView;
    private final ResourceManagementView resourceManagementView;
    private final ReportsManagementView reportsManagementView;
    private final ApplicationRole role;

    public MainMenuView(ConsoleHelper console,
                        StudentManagementView studentManagementView,
                        AcademicManagementView academicManagementView,
                        TeacherManagementView teacherManagementView,
                        ExtracurricularManagementView extracurricularManagementView,
                        FinancialManagementView financialManagementView,
                        ResourceManagementView resourceManagementView,
                        ReportsManagementView reportsManagementView,
                        ApplicationRole role) {
        this.console = console;
        this.studentManagementView = studentManagementView;
        this.academicManagementView = academicManagementView;
        this.teacherManagementView = teacherManagementView;
        this.extracurricularManagementView = extracurricularManagementView;
        this.financialManagementView = financialManagementView;
        this.resourceManagementView = resourceManagementView;
        this.reportsManagementView = reportsManagementView;
        this.role = role;
    }

    public void show() {
        boolean running = true;
        while (running) {
            printHeader();
            List<Runnable> actions = availableActions();
            int option = console.readIntInRange("Select an option: ", 1, actions.size() + 1);
            if (option == actions.size() + 1) {
                console.printInfo("Closing Little Horizons ERP. Goodbye!");
                running = false;
            } else {
                actions.get(option - 1).run();
            }
        }
    }

    private List<Runnable> availableActions() {
        List<Runnable> actions = new ArrayList<>();
        if (role.canAccessMenuOption(1)) {
            actions.add(studentManagementView::show);
        }
        if (role.canAccessMenuOption(2)) {
            actions.add(academicManagementView::show);
        }
        if (role.canAccessMenuOption(3)) {
            actions.add(teacherManagementView::show);
        }
        if (role.canAccessMenuOption(4)) {
            actions.add(extracurricularManagementView::show);
        }
        if (role.canAccessMenuOption(5)) {
            actions.add(financialManagementView::show);
        }
        if (role.canAccessMenuOption(6)) {
            actions.add(resourceManagementView::show);
        }
        if (role.canAccessMenuOption(7)) {
            actions.add(reportsManagementView::show);
        }
        return actions;
    }

    private void printHeader() {
        System.out.println();
        System.out.println("+----------------------------------------------+");
        System.out.println("|   ERP Colegio Little Horizons                |");
        System.out.println("|   Educational Management System              |");
        System.out.println("+----------------------------------------------+");
        System.out.println();
        System.out.println("Signed in role: " + role);
        int option = 1;
        if (role.canAccessMenuOption(1)) {
            System.out.println((option++) + ". Student Management");
        }
        if (role.canAccessMenuOption(2)) {
            System.out.println((option++) + ". Academic Management");
        }
        if (role.canAccessMenuOption(3)) {
            System.out.println((option++) + ". Teacher Management");
        }
        if (role.canAccessMenuOption(4)) {
            System.out.println((option++) + ". Extracurricular Activities");
        }
        if (role.canAccessMenuOption(5)) {
            System.out.println((option++) + ". Financial Management");
        }
        if (role.canAccessMenuOption(6)) {
            System.out.println((option++) + ". Resource Management");
        }
        if (role.canAccessMenuOption(7)) {
            System.out.println((option++) + ". Reports and Statistics");
        }
        System.out.println(option + ". Exit");
        System.out.println();
    }
}
