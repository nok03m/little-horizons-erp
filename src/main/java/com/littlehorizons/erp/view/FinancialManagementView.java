package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinancialController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentObligation;
import com.littlehorizons.erp.model.StudentConceptRate;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Console UI for payment concepts, payments and TXT invoices.
 */
public class FinancialManagementView {

    private static final List<String> PAYMENT_METHODS = List.of(
            "Efectivo", "Transferencia", "Tarjeta Debito", "Tarjeta Credito", "Cheque");
    private static final List<String> PAYMENT_PERIODS = List.of("I", "II", "III", "IV", "Anual");

    private final ConsoleHelper console;
    private final FinancialController financialController;
    private final StudentController studentController;
    private final GuardianController guardianController;
    private final String authenticatedUsername;

    public FinancialManagementView(ConsoleHelper console,
                                   FinancialController financialController,
                                   StudentController studentController,
                                   GuardianController guardianController,
                                   String authenticatedUsername) {
        this.console = console;
        this.financialController = financialController;
        this.studentController = studentController;
        this.guardianController = guardianController;
        this.authenticatedUsername = authenticatedUsername;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Financial Management ---");
            System.out.println("1. Payment concepts");
            System.out.println("2. Payments");
            System.out.println("3. Invoices (PDF + TXT)");
            System.out.println("4. Delinquent students report");
            System.out.println("5. Payment obligations");
            System.out.println("6. Back to main menu");
            int option = console.readIntInRange("Select an option: ", 1, 6);
            switch (option) {
                case 1 -> showConcepts();
                case 2 -> showPayments();
                case 3 -> showInvoices();
                case 4 -> showDelinquents();
                case 5 -> showObligations();
                case 6 -> running = false;
                default -> console.printError("Unexpected option.");
            }
        }
    }

    private void showConcepts() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Payment Concepts ---");
            System.out.println("1. List all");
            System.out.println("2. Create");
            System.out.println("3. Update");
            System.out.println("4. Delete");
            System.out.println("5. Back");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            try {
                switch (option) {
                    case 1 -> {
                        financialController.listConcepts().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        PaymentConcept concept = readConcept();
                        int id = financialController.createConcept(concept);
                        console.printSuccess("Concept created with ID " + id);
                        console.pause();
                    }
                    case 3 -> updateConcept();
                    case 4 -> {
                        int id = console.readPositiveInt("Concept ID: ");
                        if (console.confirm("Delete concept?")) {
                            if (financialController.deleteConcept(id)) {
                                console.printSuccess("Concept deleted.");
                            } else {
                                console.printError("Concept not found.");
                            }
                        }
                        console.pause();
                    }
                    case 5 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Concepts menu failed", exception);
                console.pause();
            }
        }
    }

    private void updateConcept() {
        int id = console.readPositiveInt("Concept ID: ");
        financialController.getConcept(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            PaymentConcept updated = readConcept();
            updated.setId(id);
            if (financialController.updateConcept(updated)) {
                console.printSuccess("Concept updated.");
            } else {
                console.printError("Concept was not updated.");
            }
        }, () -> console.printError("Concept not found."));
        console.pause();
    }

    private PaymentConcept readConcept() {
        PaymentConcept concept = new PaymentConcept();
        concept.setCode(console.readRequired("Concept code: "));
        concept.setName(console.readRequired("Concept name: "));
        concept.setDescription(console.readOptional("Description (optional): "));
        concept.setBaseAmount(console.readMoney("Base amount"));
        concept.setRecurrent(console.confirm("Is recurrent?"));
        concept.setActive(console.confirm("Is active?"));
        return concept;
    }

    private void showPayments() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Payments ---");
            System.out.println("1. List all");
            System.out.println("2. List by student");
            System.out.println("3. List by guardian");
            System.out.println("4. Register payment (+ automatic invoice)");
            System.out.println("5. Update");
            System.out.println("6. Allocate/retry a payment against obligations");
            System.out.println("7. Back");
            int option = console.readIntInRange("Select an option: ", 1, 7);
            try {
                switch (option) {
                    case 1 -> {
                        financialController.listPayments().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int studentId = console.readPositiveInt("Student ID: ");
                        financialController.listPaymentsByStudent(studentId).forEach(System.out::println);
                        console.pause();
                    }
                    case 3 -> {
                        int guardianId = console.readPositiveInt("Guardian ID: ");
                        financialController.listPaymentsByGuardian(guardianId).forEach(System.out::println);
                        console.pause();
                    }
                    case 4 -> registerPayment();
                    case 5 -> updatePayment();
                    case 6 -> allocatePayment();
                    case 7 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Payments menu failed", exception);
                console.pause();
            }
        }
    }

    private void registerPayment() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) {
            console.printError("Student not found.");
            console.pause();
            return;
        }
        financialController.listConcepts().forEach(System.out::println);
        int conceptId = console.readPositiveInt("Concept ID: ");
        PaymentConcept concept = financialController.getConcept(conceptId)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found."));

        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setConceptId(conceptId);

        if (console.confirm("Link guardian/payee?")) {
            int guardianId = console.readPositiveInt("Guardian ID: ");
            if (guardianController.getGuardian(guardianId).isEmpty()) {
                throw new IllegalArgumentException("Guardian not found.");
            }
            payment.setGuardianId(guardianId);
        }

        LocalDate paymentDate = console.readDate("Payment date");
        var applicableRate = financialController.getApplicableRate(studentId, conceptId, paymentDate);
        applicableRate.ifPresent(rate -> console.printInfo("Applicable student rate: " + rate.amount()
                + " (" + rate.reason() + ", authorized by " + rate.authorizedBy() + ")"));
        System.out.println("Base amount: " + concept.getBaseAmount());
        BigDecimal amount;
        StudentConceptRate rateToSave = null;
        if (applicableRate.isPresent() && console.confirm("Use applicable student rate?")) {
            amount = applicableRate.get().amount();
            payment.setRateId(applicableRate.get().id());
        } else if (console.confirm("Use base amount?")) {
            amount = concept.getBaseAmount();
        } else {
            amount = console.readMoney("Custom amount");
            if (console.confirm("Save this as a student rate for future payments?")) {
                LocalDate effectiveUntil = console.readOptionalDate("Rate valid until");
                String reason = console.readRequired("Rate reason (e.g. sibling discount or scholarship): ");
                rateToSave = new StudentConceptRate(0, studentId, conceptId, amount,
                        paymentDate, effectiveUntil, reason, authenticatedUsername);
            }
        }
        payment.setAmountPaid(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMethod(console.readChoice("Payment method:", PAYMENT_METHODS));
        payment.setReceiptNumber(console.readOptional("Receipt number (optional): "));
        if (console.confirm("Set academic year/period?")) {
            payment.setAcademicYear(console.readIntInRange("Academic year: ", 2000, 2100));
            payment.setAcademicPeriod(console.readChoice("Period:", PAYMENT_PERIODS));
        }
        payment.setNotes(console.readOptional("Notes (optional): "));

        int paymentId = financialController.registerPayment(payment);
        console.printSuccess("Payment registered with ID " + paymentId);
        try {
            BigDecimal applied = financialController.allocatePayment(paymentId);
            if (applied.signum() > 0) console.printSuccess("Applied " + applied + " to oldest outstanding obligations.");
        } catch (RuntimeException exception) {
            AppLogger.error("Payment saved but obligation allocation failed for payment " + paymentId, exception);
            console.printError("Payment is saved; obligation allocation can be retried from the Payments menu.");
        }
        if (rateToSave != null) {
            try {
                int rateId = financialController.saveStudentRate(rateToSave);
                payment.setRateId(rateId);
                financialController.attachRateToPayment(paymentId, rateId);
                console.printSuccess("Student rate saved with ID " + rateId);
            } catch (RuntimeException exception) {
                AppLogger.error("Payment saved but student rate could not be persisted", exception);
                console.printError("Payment is saved, but the student rate could not be saved: " + exception.getMessage());
            }
        }
        try {
            Invoice invoice = financialController.generateInvoiceForPayment(paymentId);
            console.printSuccess("Invoice " + invoice.getInvoiceNumber() + " (PDF) saved at:");
            console.printInfo(invoice.getFilePath());
            console.printInfo("TXT companion: same folder, .txt extension");
        } catch (RuntimeException exception) {
            AppLogger.error("Payment saved but automatic invoice generation failed for payment " + paymentId, exception);
            console.printError("Payment was saved, but invoice generation failed. Retry it from Invoices using payment ID " + paymentId + ".");
        }
        console.pause();
    }

    private void updatePayment() {
        int id = console.readPositiveInt("Payment ID: ");
        financialController.getPayment(id).ifPresentOrElse(existing -> {
            System.out.println(existing);
            existing.setAmountPaid(console.readMoney("Amount"));
            existing.setPaymentDate(console.readDate("Payment date"));
            existing.setPaymentMethod(console.readChoice("Payment method:", PAYMENT_METHODS));
            existing.setReceiptNumber(console.readOptional("Receipt number (optional): "));
            existing.setNotes(console.readOptional("Notes (optional): "));
            if (financialController.updatePayment(existing)) {
                console.printSuccess("Payment updated.");
            } else {
                console.printError("Payment was not updated.");
            }
        }, () -> console.printError("Payment not found."));
        console.pause();
    }

    private void showInvoices() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Invoices (PDF primary + TXT companion) ---");
            System.out.println("1. List all");
            System.out.println("2. View by ID");
            System.out.println("3. Retry all pending invoices");
            System.out.println("4. Generate/retrieve invoice for a payment");
            System.out.println("5. Back");
            int option = console.readIntInRange("Select an option: ", 1, 5);
            try {
                switch (option) {
                    case 1 -> {
                        financialController.listInvoices().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> {
                        int id = console.readPositiveInt("Invoice ID: ");
                        financialController.getInvoice(id).ifPresentOrElse(invoice -> {
                            System.out.println(invoice);
                            console.printInfo("File: " + invoice.getFilePath());
                            console.printInfo("CUFE: " + invoice.getCufe());
                        }, () -> console.printError("Invoice not found."));
                        console.pause();
                    }
                    case 3 -> {
                        List<Integer> pending = financialController.listPendingInvoicePaymentIds();
                        if (pending.isEmpty()) console.printInfo("No invoices are pending.");
                        for (int paymentId : pending) {
                            try {
                                Invoice invoice = financialController.generateInvoiceForPayment(paymentId);
                                console.printSuccess("Payment " + paymentId + " -> " + invoice.getInvoiceNumber());
                            } catch (RuntimeException exception) {
                                console.printError("Payment " + paymentId + " is still pending: " + exception.getMessage());
                            }
                        }
                        console.pause();
                    }
                    case 4 -> {
                        int paymentId = console.readPositiveInt("Payment ID: ");
                        Invoice invoice = financialController.generateInvoiceForPayment(paymentId);
                        console.printSuccess("Invoice " + invoice.getInvoiceNumber() + " is available.");
                        console.printInfo(invoice.getFilePath());
                        console.pause();
                    }
                    case 5 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Invoices menu failed", exception);
                console.pause();
            }
        }
    }

    private void showDelinquents() {
        try {
            int days = console.readIntInRange("Minimum days overdue (30 = more than one month): ", 1, 365);
            var delinquents = financialController.listDelinquentStudents(days);
            if (delinquents.isEmpty()) {
                console.printInfo("No delinquent students found.");
            } else {
                delinquents.forEach(System.out::println);
                console.printInfo("Total: " + delinquents.size());
            }
        } catch (RuntimeException exception) {
            console.printError(exception.getMessage());
            AppLogger.error("Delinquent report failed", exception);
        }
        console.pause();
    }

    private void allocatePayment() {
        int paymentId = console.readPositiveInt("Payment ID to allocate (unallocated amount only): ");
        BigDecimal amount = financialController.allocatePayment(paymentId);
        console.printSuccess("Applied " + amount + " to oldest outstanding obligations.");
        console.pause();
    }

    private void showObligations() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("--- Payment Obligations ---");
            System.out.println("1. List outstanding obligations");
            System.out.println("2. Create obligation");
            System.out.println("3. Allocate an existing payment");
            System.out.println("4. Back");
            int option = console.readIntInRange("Select an option: ", 1, 4);
            try {
                switch (option) {
                    case 1 -> {
                        financialController.listOutstandingObligations().forEach(System.out::println);
                        console.pause();
                    }
                    case 2 -> createObligation();
                    case 3 -> allocatePayment();
                    case 4 -> running = false;
                    default -> console.printError("Unexpected option.");
                }
            } catch (RuntimeException exception) {
                console.printError(exception.getMessage());
                AppLogger.error("Payment obligations menu failed", exception);
                console.pause();
            }
        }
    }

    private void createObligation() {
        int studentId = console.readPositiveInt("Student ID: ");
        if (studentController.getStudent(studentId).isEmpty()) throw new IllegalArgumentException("Student not found.");
        financialController.listConcepts().forEach(System.out::println);
        int conceptId = console.readPositiveInt("Concept ID: ");
        if (financialController.getConcept(conceptId).isEmpty()) throw new IllegalArgumentException("Concept not found.");
        String periodKey = console.readMatching("Period key (e.g. 2026-03): ",
                value -> value.matches("[A-Za-z0-9_-]{1,20}"), "Use 1-20 letters, numbers, hyphens or underscores.");
        LocalDate dueDate = console.readDate("Due date");
        BigDecimal amount = console.readMoney("Amount due");
        String description = console.readRequired("Description: ");
        PaymentObligation obligation = new PaymentObligation(0, studentId, conceptId, periodKey,
                dueDate, amount, BigDecimal.ZERO, description, authenticatedUsername);
        int id = financialController.createObligation(obligation);
        console.printSuccess("Obligation created with ID " + id);
        console.pause();
    }
}
