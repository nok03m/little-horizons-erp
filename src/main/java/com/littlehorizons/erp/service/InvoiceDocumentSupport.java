package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * Shared constants and helpers for simulated DIAN invoices (TXT/PDF).
 */
final class InvoiceDocumentSupport {

    static final String SCHOOL_NAME = "Colegio Internacional Little Horizons S.A.S.";
    static final String SCHOOL_NIT = "900123456-7";
    static final String SCHOOL_ADDRESS = "Calle 100 #15-20, Bogota D.C.";
    static final String SCHOOL_PHONE = "+57 601 555 0100";
    static final String SCHOOL_EMAIL = "facturacion@littlehorizons.edu.co";
    static final String RESOLUTION = "Resolucion DIAN simulada 18760000001 de 2026";
    static final Path INVOICES_DIR = Path.of("invoices");
    static final BigDecimal IVA_RATE = new BigDecimal("0.19");

    private InvoiceDocumentSupport() {
    }

    static String invoiceNumber(int sequence) {
        return String.format("FE-2026-%06d", sequence);
    }

    static String buildCufe(String invoiceNumber, String nit, String date) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((invoiceNumber + "|" + nit + "|" + date).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 64);
        } catch (NoSuchAlgorithmException exception) {
            return "CUFE-SIM-" + invoiceNumber.replace("-", "");
        }
    }

    static BigDecimal subtotal(Payment payment) {
        return payment.getAmountPaid().setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal ivaAmount() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    static String clientName(Student student, Guardian guardian) {
        return guardian != null ? guardian.getFullName() : student.getFullName();
    }

    static String clientDocument(Student student, Guardian guardian) {
        if (guardian != null) {
            return guardian.getDocumentType() + " " + guardian.getDocumentNumber();
        }
        return student.getDocumentType() + " " + student.getDocumentNumber();
    }

    static String clientAddress(Student student, Guardian guardian) {
        if (guardian != null && guardian.getAddress() != null && !guardian.getAddress().isBlank()) {
            return guardian.getAddress();
        }
        return student.getAddress() == null || student.getAddress().isBlank() ? "N/A" : student.getAddress();
    }

    static String issueDate() {
        return LocalDate.now().toString();
    }

    /**
     * Payload encoded in the simulated DIAN QR (invoice key fields + CUFE).
     */
    static String qrPayload(String invoiceNumber,
                            String cufe,
                            BigDecimal total,
                            String clientDocument) {
        return "NumFac:" + invoiceNumber
                + "\nFecFac:" + issueDate()
                + "\nNitFac:" + SCHOOL_NIT
                + "\nDocAdq:" + clientDocument
                + "\nValFac:" + total.toPlainString()
                + "\nCUFE:" + cufe;
    }
}
