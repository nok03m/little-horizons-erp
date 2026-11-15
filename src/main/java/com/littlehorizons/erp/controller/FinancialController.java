package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.PaymentObligationDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentConceptRateDao;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentObligation;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentConceptRate;
import com.littlehorizons.erp.service.InvoicePdfService;
import com.littlehorizons.erp.service.InvoiceTxtService;
import com.littlehorizons.erp.util.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FinancialController {

    private final PaymentConceptDao paymentConceptDao;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final StudentDao studentDao;
    private final StudentConceptRateDao studentConceptRateDao;
    private final PaymentObligationDao paymentObligationDao;
    private final GuardianDao guardianDao;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceTxtService invoiceTxtService;

    public FinancialController(PaymentConceptDao paymentConceptDao,
                               PaymentDao paymentDao,
                               InvoiceDao invoiceDao,
                               StudentDao studentDao,
                               StudentConceptRateDao studentConceptRateDao,
                               PaymentObligationDao paymentObligationDao,
                               GuardianDao guardianDao,
                               InvoicePdfService invoicePdfService,
                               InvoiceTxtService invoiceTxtService) {
        this.paymentConceptDao = paymentConceptDao;
        this.paymentDao = paymentDao;
        this.invoiceDao = invoiceDao;
        this.studentDao = studentDao;
        this.studentConceptRateDao = studentConceptRateDao;
        this.paymentObligationDao = paymentObligationDao;
        this.guardianDao = guardianDao;
        this.invoicePdfService = invoicePdfService;
        this.invoiceTxtService = invoiceTxtService;
    }

    public List<PaymentConcept> listConcepts() {
        return paymentConceptDao.findAll();
    }

    public Optional<PaymentConcept> getConcept(int id) {
        return paymentConceptDao.findById(id);
    }

    public int createConcept(PaymentConcept concept) {
        return paymentConceptDao.insert(concept);
    }

    public boolean updateConcept(PaymentConcept concept) {
        return paymentConceptDao.update(concept);
    }

    public boolean deleteConcept(int id) {
        return paymentConceptDao.deleteById(id);
    }

    public List<Payment> listPayments() {
        return paymentDao.findAll();
    }

    public List<Payment> listPaymentsByStudent(int studentId) {
        return paymentDao.findByStudentId(studentId);
    }

    public List<Payment> listPaymentsByGuardian(int guardianId) {
        return paymentDao.findByGuardianId(guardianId);
    }

    public Optional<Payment> getPayment(int id) {
        return paymentDao.findById(id);
    }

    public int registerPayment(Payment payment) {
        return paymentDao.insert(payment);
    }

    public int createObligation(PaymentObligation obligation) {
        return paymentObligationDao.insert(obligation);
    }

    public List<PaymentObligation> listOutstandingObligations() {
        return paymentObligationDao.findOutstanding();
    }

    public java.math.BigDecimal allocatePayment(int paymentId) {
        return paymentObligationDao.allocateToOldestObligations(paymentId);
    }

    public boolean hasPendingInvoice(int paymentId) {
        return paymentDao.findPaymentsPendingInvoice().contains(paymentId);
    }

    public List<Integer> listPendingInvoicePaymentIds() {
        return paymentDao.findPaymentsPendingInvoice();
    }

    public Optional<StudentConceptRate> getApplicableRate(int studentId, int conceptId, LocalDate date) {
        return studentConceptRateDao.findApplicable(studentId, conceptId, date);
    }

    public int saveStudentRate(StudentConceptRate rate) {
        return studentConceptRateDao.insert(rate);
    }

    public void attachRateToPayment(int paymentId, int rateId) {
        paymentDao.attachRateToPayment(paymentId, rateId);
    }

    public boolean updatePayment(Payment payment) {
        if (invoiceDao.findByPaymentId(payment.getId()).isPresent()) {
            throw new IllegalStateException("A payment with an issued invoice cannot be edited; record a reversal/adjustment instead.");
        }
        if (paymentObligationDao.hasAllocationsForPayment(payment.getId())) {
            throw new IllegalStateException("A payment allocated to obligations cannot be edited; record a reversal/adjustment instead.");
        }
        return paymentDao.update(payment);
    }

    public List<Invoice> listInvoices() {
        return invoiceDao.findAll();
    }

    public Optional<Invoice> getInvoice(int id) {
        return invoiceDao.findById(id);
    }

    public Optional<Invoice> getInvoiceByPayment(int paymentId) {
        return invoiceDao.findByPaymentId(paymentId);
    }

    /**
     * Generates the official PDF invoice (stored in DB) and a TXT companion copy.
     */
    public Invoice generateInvoiceForPayment(int paymentId) {
        Optional<Invoice> existingInvoice = invoiceDao.findByPaymentId(paymentId);
        if (existingInvoice.isPresent()) return existingInvoice.get();
        Payment payment = paymentDao.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        PaymentConcept concept = paymentConceptDao.findById(payment.getConceptId())
                .orElseThrow(() -> new IllegalArgumentException("Payment concept not found."));
        Student student = studentDao.findById(payment.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        Guardian guardian = null;
        if (payment.getGuardianId() != null) {
            guardian = guardianDao.findById(payment.getGuardianId()).orElse(null);
        }

        int sequence = invoiceDao.nextSequence();
        Invoice invoice = invoicePdfService.generate(payment, concept, student, guardian, sequence);
        try {
            invoiceDao.insert(invoice);
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(Path.of(invoice.getFilePath()));
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
                AppLogger.error("Failed to remove an invoice PDF after database persistence failed.", cleanupException);
            }
            Optional<Invoice> concurrentlyIssued = invoiceDao.findByPaymentId(paymentId);
            if (concurrentlyIssued.isPresent()) return concurrentlyIssued.get();
            throw exception;
        }

        try {
            Path txtPath = invoiceTxtService.writeCompanionTxt(invoice, payment, concept, student, guardian);
            AppLogger.info("Companion TXT invoice written: " + txtPath);
        } catch (RuntimeException exception) {
            AppLogger.error("PDF invoice saved but TXT companion failed", exception);
        }
        return invoice;
    }

    public List<DelinquentStudent> listDelinquentStudents(int daysThreshold) {
        return paymentObligationDao.findDelinquentStudents(daysThreshold);
    }
}
