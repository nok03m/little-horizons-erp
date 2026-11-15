package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppLogger;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates simulated electronic invoices as TXT files (DIAN-like fields).
 */
public class InvoiceTxtService {

    public Invoice generate(Payment payment,
                            PaymentConcept concept,
                            Student student,
                            Guardian guardian,
                            int sequence) {
        try {
            Files.createDirectories(InvoiceDocumentSupport.INVOICES_DIR);
            String invoiceNumber = InvoiceDocumentSupport.invoiceNumber(sequence);
            String fileName = String.format("factura_%06d.txt", sequence);
            Path filePath = InvoiceDocumentSupport.INVOICES_DIR.resolve(fileName);
            String cufe = InvoiceDocumentSupport.buildCufe(
                    invoiceNumber, InvoiceDocumentSupport.SCHOOL_NIT, InvoiceDocumentSupport.issueDate());

            BigDecimal subtotal = InvoiceDocumentSupport.subtotal(payment);
            BigDecimal iva = InvoiceDocumentSupport.ivaAmount();
            BigDecimal total = subtotal.add(iva);

            String clientName = InvoiceDocumentSupport.clientName(student, guardian);
            String clientDocument = InvoiceDocumentSupport.clientDocument(student, guardian);
            String clientAddress = InvoiceDocumentSupport.clientAddress(student, guardian);

            String content = buildContent(
                    invoiceNumber, cufe, payment, concept, student,
                    clientName, clientDocument, clientAddress, subtotal, iva, total);

            Files.writeString(filePath, content, StandardCharsets.UTF_8);

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setPaymentId(payment.getId());
            invoice.setIssueDate(LocalDate.now());
            invoice.setCufe(cufe);
            invoice.setFileName(fileName);
            invoice.setFilePath(filePath.toAbsolutePath().toString());
            return invoice;
        } catch (IOException exception) {
            AppLogger.error("Failed to write invoice TXT", exception);
            throw new IllegalStateException("Unable to generate invoice TXT file.", exception);
        }
    }

    /**
     * Writes a TXT companion using the same invoice number/CUFE already persisted for PDF.
     */
    public Path writeCompanionTxt(Invoice pdfInvoice,
                                  Payment payment,
                                  PaymentConcept concept,
                                  Student student,
                                  Guardian guardian) {
        try {
            Files.createDirectories(InvoiceDocumentSupport.INVOICES_DIR);
            String baseName = pdfInvoice.getFileName().replace(".pdf", ".txt");
            if (!baseName.endsWith(".txt")) {
                baseName = pdfInvoice.getInvoiceNumber().replace("FE-2026-", "factura_") + ".txt";
            }
            Path filePath = InvoiceDocumentSupport.INVOICES_DIR.resolve(baseName);
            BigDecimal subtotal = InvoiceDocumentSupport.subtotal(payment);
            BigDecimal iva = InvoiceDocumentSupport.ivaAmount();
            BigDecimal total = subtotal.add(iva);
            String content = buildContent(
                    pdfInvoice.getInvoiceNumber(),
                    pdfInvoice.getCufe(),
                    payment,
                    concept,
                    student,
                    InvoiceDocumentSupport.clientName(student, guardian),
                    InvoiceDocumentSupport.clientDocument(student, guardian),
                    InvoiceDocumentSupport.clientAddress(student, guardian),
                    subtotal,
                    iva,
                    total);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath.toAbsolutePath();
        } catch (IOException exception) {
            AppLogger.error("Failed to write companion TXT invoice", exception);
            throw new IllegalStateException("Unable to generate companion TXT invoice.", exception);
        }
    }

    private String buildContent(String invoiceNumber,
                                String cufe,
                                Payment payment,
                                PaymentConcept concept,
                                Student student,
                                String clientName,
                                String clientDocument,
                                String clientAddress,
                                BigDecimal subtotal,
                                BigDecimal iva,
                                BigDecimal total) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder builder = new StringBuilder();
        builder.append("============================================================\n");
        builder.append("           FACTURA ELECTRONICA (SIMULADA DIAN)\n");
        builder.append("============================================================\n");
        builder.append("Emisor: ").append(InvoiceDocumentSupport.SCHOOL_NAME).append('\n');
        builder.append("NIT: ").append(InvoiceDocumentSupport.SCHOOL_NIT).append('\n');
        builder.append("Direccion: ").append(InvoiceDocumentSupport.SCHOOL_ADDRESS).append('\n');
        builder.append("Telefono: ").append(InvoiceDocumentSupport.SCHOOL_PHONE).append('\n');
        builder.append("Correo: ").append(InvoiceDocumentSupport.SCHOOL_EMAIL).append('\n');
        builder.append(InvoiceDocumentSupport.RESOLUTION).append('\n');
        builder.append("------------------------------------------------------------\n");
        builder.append("Numero factura: ").append(invoiceNumber).append('\n');
        builder.append("Fecha emision: ").append(LocalDate.now().format(formatter)).append('\n');
        builder.append("CUFE: ").append(cufe).append('\n');
        builder.append("QR simulado: [QR]").append(cufe).append("[/QR]\n");
        builder.append("------------------------------------------------------------\n");
        builder.append("Cliente / Acudiente: ").append(clientName).append('\n');
        builder.append("Documento: ").append(clientDocument).append('\n');
        builder.append("Direccion: ").append(clientAddress).append('\n');
        builder.append("Estudiante: ").append(student.getFullName())
                .append(" (").append(student.getDocumentType()).append(' ')
                .append(student.getDocumentNumber()).append(")\n");
        builder.append("------------------------------------------------------------\n");
        builder.append(String.format("%-30s %8s %12s %12s%n", "Descripcion", "Cant", "V.Unit", "Subtotal"));
        builder.append(String.format("%-30s %8d %12.2f %12.2f%n",
                concept.getName(), 1, payment.getAmountPaid(), subtotal));
        builder.append("------------------------------------------------------------\n");
        builder.append(String.format("Subtotal: %s%n", subtotal));
        builder.append(String.format("IVA (%.0f%%, si aplica): %s%n",
                InvoiceDocumentSupport.IVA_RATE.multiply(new BigDecimal("100")), iva));
        builder.append(String.format("TOTAL: %s%n", total));
        builder.append("Metodo de pago: ").append(payment.getPaymentMethod()).append('\n');
        builder.append("Recibo: ").append(payment.getReceiptNumber() == null ? "N/A" : payment.getReceiptNumber()).append('\n');
        builder.append("Fecha pago: ").append(payment.getPaymentDate()).append('\n');
        builder.append("============================================================\n");
        builder.append("Documento generado por Little Horizons ERP (modo simulacion)\n");
        return builder.toString();
    }
}
