package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinancialController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.ReportsController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.PaymentObligationDao;
import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.SchoolEventDao;
import com.littlehorizons.erp.dao.SchoolResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentConceptRateDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.dao.StudentLoanDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeacherLoanDao;
import com.littlehorizons.erp.dao.UserAccountDao;
import com.littlehorizons.erp.model.ApplicationRole;
import com.littlehorizons.erp.model.UserAccount;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AuthService;
import com.littlehorizons.erp.service.InvoicePdfService;
import com.littlehorizons.erp.service.InvoiceTxtService;
import com.littlehorizons.erp.service.ReportTxtService;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.AcademicManagementView;
import com.littlehorizons.erp.view.ExtracurricularManagementView;
import com.littlehorizons.erp.view.FinancialManagementView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ReportsManagementView;
import com.littlehorizons.erp.view.ResourceManagementView;
import com.littlehorizons.erp.view.StudentManagementView;
import com.littlehorizons.erp.view.TeacherManagementView;

import java.util.Scanner;
import java.util.Optional;

/**
 * Application entry point for Little Horizons ERP.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Starting Little Horizons ERP...");
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

        if (!databaseConnection.testConnection()) {
            System.err.println("Unable to connect to MySQL.");
            System.err.println("URL: " + databaseConnection.getJdbcUrl());
            System.err.println("Check application.properties and that MySQL is running.");
            System.err.println("Details were written to error_log.txt");
            AppLogger.error("Application aborted because the database is unreachable.");
            return;
        }

        System.out.println("Database connection OK: " + databaseConnection.getJdbcUrl());
        AppLogger.info("Application started successfully.");

        java.io.Console terminal = System.console();
        if (terminal == null) {
            System.err.println("A secure terminal is required for password entry. Run the application from a terminal.");
            return;
        }

        UserAccountDao userAccountDao = new UserAccountDao();
        AuthService authService = new AuthService(userAccountDao);
        int accountCount;
        try {
            accountCount = userAccountDao.countAccounts();
        } catch (IllegalStateException exception) {
            System.err.println("Application account table is missing. Apply docs/migrations/002_user_accounts.sql.");
            AppLogger.error("Application account schema is unavailable.", exception);
            return;
        }

        if (args.length > 0) {
            boolean accountStatusCommand = args.length == 2
                    && ("--disable-user".equals(args[0]) || "--enable-user".equals(args[0]));
            if (args.length != 1 && !accountStatusCommand) {
                System.err.println("Use --bootstrap-admin, --create-user, --disable-user <name> or --enable-user <name>.");
                return;
            }
            if (accountStatusCommand) {
                if (accountCount == 0) {
                    System.err.println("No accounts exist. Create the first administrator with --bootstrap-admin first.");
                    return;
                }
                try {
                    boolean active = "--enable-user".equals(args[0]);
                    boolean changed = authService.setAccountActiveAsAdministrator(terminal, args[1], active);
                    System.out.println(changed ? "Account status updated." : "Account not found.");
                } catch (RuntimeException exception) {
                    System.err.println(exception.getMessage());
                    AppLogger.error("Failed to change account status.", exception);
                }
                return;
            }
            switch (args[0]) {
                case "--bootstrap-admin" -> {
                    if (accountCount != 0) {
                        System.err.println("Accounts already exist. Administrator bootstrap is disabled.");
                        return;
                    }
                    authService.bootstrapAdministrator(terminal);
                }
                case "--create-user" -> {
                    if (accountCount == 0) {
                        System.err.println("Create the first administrator with --bootstrap-admin first.");
                        return;
                    }
                    authService.createAccountAsAdministrator(terminal);
                }
                default -> System.err.println("Unknown command. Use --bootstrap-admin, --create-user, --disable-user or --enable-user.");
            }
            return;
        }

        if (accountCount == 0) {
            System.err.println("No ERP accounts exist. Create the first administrator with --bootstrap-admin.");
            return;
        }

        Optional<UserAccount> authenticated = authService.login(terminal);
        if (authenticated.isEmpty()) {
            AppLogger.warning("Application login failed or was cancelled.");
            return;
        }
        ApplicationRole role = authenticated.get().role();
        AppLogger.info("Authenticated console session started with role " + role + ".");

        StudentDao studentDao = new StudentDao();
        GuardianDao guardianDao = new GuardianDao();
        StudentGuardianDao studentGuardianDao = new StudentGuardianDao();
        EnrollmentDao enrollmentDao = new EnrollmentDao();
        GradeDao gradeDao = new GradeDao();
        TeacherDao teacherDao = new TeacherDao();
        TeacherAssignmentDao teacherAssignmentDao = new TeacherAssignmentDao();
        SubjectDao subjectDao = new SubjectDao();
        SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDao();
        AcademicRecordDao academicRecordDao = new AcademicRecordDao();
        AttendanceDao attendanceDao = new AttendanceDao();
        AcademicService academicService = new AcademicService();

        StudentController studentController = new StudentController(studentDao);
        GuardianController guardianController = new GuardianController(guardianDao, studentGuardianDao);
        EnrollmentController enrollmentController = new EnrollmentController(enrollmentDao, gradeDao);
        TeacherController teacherController = new TeacherController(teacherDao, teacherAssignmentDao);
        AcademicController academicController = new AcademicController(
                subjectDao, subjectEnrollmentDao, academicRecordDao, attendanceDao, gradeDao, academicService);

        ClubDao clubDao = new ClubDao();
        ClubMembershipDao clubMembershipDao = new ClubMembershipDao();
        SchoolEventDao schoolEventDao = new SchoolEventDao();
        EventParticipantDao eventParticipantDao = new EventParticipantDao();
        ExtracurricularController extracurricularController = new ExtracurricularController(
                clubDao, clubMembershipDao, schoolEventDao, eventParticipantDao);

        PaymentConceptDao paymentConceptDao = new PaymentConceptDao();
        PaymentDao paymentDao = new PaymentDao();
        PaymentObligationDao paymentObligationDao = new PaymentObligationDao();
        InvoiceDao invoiceDao = new InvoiceDao();
        StudentConceptRateDao studentConceptRateDao = new StudentConceptRateDao();
        InvoicePdfService invoicePdfService = new InvoicePdfService();
        InvoiceTxtService invoiceTxtService = new InvoiceTxtService();
        FinancialController financialController = new FinancialController(
                paymentConceptDao, paymentDao, invoiceDao, studentDao,
                studentConceptRateDao, paymentObligationDao, guardianDao,
                invoicePdfService, invoiceTxtService);

        ResourceCategoryDao resourceCategoryDao = new ResourceCategoryDao();
        SchoolResourceDao schoolResourceDao = new SchoolResourceDao();
        StudentLoanDao studentLoanDao = new StudentLoanDao();
        TeacherLoanDao teacherLoanDao = new TeacherLoanDao();
        ResourceController resourceController = new ResourceController(
                resourceCategoryDao, schoolResourceDao, studentLoanDao, teacherLoanDao);

        ReportTxtService reportTxtService = new ReportTxtService();
        ReportsController reportsController = new ReportsController(
                studentController, teacherController, academicController,
                extracurricularController, financialController, resourceController,
                academicService, reportTxtService);

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleHelper console = new ConsoleHelper(scanner);
            StudentManagementView studentManagementView = new StudentManagementView(
                    console, studentController, guardianController, enrollmentController);
            AcademicManagementView academicManagementView = new AcademicManagementView(
                    console, academicController, studentController, teacherController);
            TeacherManagementView teacherManagementView = new TeacherManagementView(
                    console, teacherController, academicController);
            ExtracurricularManagementView extracurricularManagementView = new ExtracurricularManagementView(
                    console, extracurricularController, studentController, teacherController);
            FinancialManagementView financialManagementView = new FinancialManagementView(
                    console, financialController, studentController, guardianController,
                    authenticated.get().username());
            ResourceManagementView resourceManagementView = new ResourceManagementView(
                    console, resourceController, studentController, teacherController);
            ReportsManagementView reportsManagementView = new ReportsManagementView(console, reportsController);
            MainMenuView mainMenuView = new MainMenuView(
                    console, studentManagementView, academicManagementView,
                    teacherManagementView, extracurricularManagementView,
                    financialManagementView, resourceManagementView, reportsManagementView, role);
            mainMenuView.show();
        }

        AppLogger.info("Application shut down cleanly.");
    }
}
