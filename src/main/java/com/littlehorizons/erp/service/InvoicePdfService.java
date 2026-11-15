package com.littlehorizons.erp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppLogger;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Generates simulated electronic invoices as PDF files (DIAN-like fields).
 */
public class InvoicePdfService {

    public Invoice generate(Payment payment,
                            PaymentConcept concept,
                            Student student,
                            Guardian guardian,
                            int sequence) {
        String invoiceNumber = InvoiceDocumentSupport.invoiceNumber(sequence);
        String fileName = String.format("factura_%06d.pdf", sequence);
        Path filePath = InvoiceDocumentSupport.INVOICES_DIR.resolve(fileName);
        String cufe = InvoiceDocumentSupport.buildCufe(
                invoiceNumber, InvoiceDocumentSupport.SCHOOL_NIT, InvoiceDocumentSupport.issueDate());

        BigDecimal subtotal = InvoiceDocumentSupport.subtotal(payment);
        BigDecimal iva = InvoiceDocumentSupport.ivaAmount();
        BigDecimal total = subtotal.add(iva);

        String clientName = InvoiceDocumentSupport.clientName(student, guardian);
        String clientDocument = InvoiceDocumentSupport.clientDocument(student, guardian);
        String clientAddress = InvoiceDocumentSupport.clientAddress(student, guardian);

        try {
            Files.createDirectories(InvoiceDocumentSupport.INVOICES_DIR);
            writePdf(filePath, invoiceNumber, cufe, payment, concept, student,
                    clientName, clientDocument, clientAddress, subtotal, iva, total);
        } catch (IOException | DocumentException exception) {
            AppLogger.error("Failed to write invoice PDF", exception);
            throw new IllegalStateException("Unable to generate invoice PDF file.", exception);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPaymentId(payment.getId());
        invoice.setIssueDate(LocalDate.now());
        invoice.setCufe(cufe);
        invoice.setFileName(fileName);
        invoice.setFilePath(filePath.toAbsolutePath().toString());
        return invoice;
    }

    private void writePdf(Path filePath,
                          String invoiceNumber,
                          String cufe,
                          Payment payment,
                          PaymentConcept concept,
                          Student student,
                          String clientName,
                          String clientDocument,
                          String clientAddress,
                          BigDecimal subtotal,
                          BigDecimal iva,
                          BigDecimal total) throws IOException, DocumentException {
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph("FACTURA ELECTRONICA (SIMULADA DIAN)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Emisor: " + InvoiceDocumentSupport.SCHOOL_NAME, normalFont));
            document.add(new Paragraph("NIT: " + InvoiceDocumentSupport.SCHOOL_NIT, normalFont));
            document.add(new Paragraph("Direccion: " + InvoiceDocumentSupport.SCHOOL_ADDRESS, normalFont));
            document.add(new Paragraph("Telefono: " + InvoiceDocumentSupport.SCHOOL_PHONE, normalFont));
            document.add(new Paragraph("Correo: " + InvoiceDocumentSupport.SCHOOL_EMAIL, normalFont));
            document.add(new Paragraph(InvoiceDocumentSupport.RESOLUTION, normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Numero factura: " + invoiceNumber, boldFont));
            document.add(new Paragraph("Fecha emision: " + LocalDate.now(), normalFont));
            document.add(new Paragraph("CUFE: " + cufe, normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Codigo QR (simulado DIAN):", boldFont));
            String qrPayload = InvoiceDocumentSupport.qrPayload(invoiceNumber, cufe, total, clientDocument);
            document.add(buildQrImage(qrPayload));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Cliente / Acudiente: " + clientName, normalFont));
            document.add(new Paragraph("Documento: " + clientDocument, normalFont));
            document.add(new Paragraph("Direccion: " + clientAddress, normalFont));
            document.add(new Paragraph(
                    "Estudiante: " + student.getFullName()
                            + " (" + student.getDocumentType() + " " + student.getDocumentNumber() + ")",
                    normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1f, 2f, 2f});
            addHeaderCell(table, "Descripcion", boldFont);
            addHeaderCell(table, "Cant", boldFont);
            addHeaderCell(table, "V.Unit", boldFont);
            addHeaderCell(table, "Subtotal", boldFont);
            addCell(table, concept.getName(), normalFont);
            addCell(table, "1", normalFont);
            addCell(table, payment.getAmountPaid().toPlainString(), normalFont);
            addCell(table, subtotal.toPlainString(), normalFont);
            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Subtotal: " + subtotal, normalFont));
            document.add(new Paragraph(
                    "IVA (" + InvoiceDocumentSupport.IVA_RATE.multiply(new BigDecimal("100")).intValue()
                            + "%, si aplica): " + iva,
                    normalFont));
            document.add(new Paragraph("TOTAL: " + total, boldFont));
            document.add(new Paragraph("Metodo de pago: " + payment.getPaymentMethod(), normalFont));
            document.add(new Paragraph(
                    "Recibo: " + (payment.getReceiptNumber() == null ? "N/A" : payment.getReceiptNumber()),
                    normalFont));
            document.add(new Paragraph("Fecha pago: " + payment.getPaymentDate(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Documento generado por Little Horizons ERP (modo simulacion)", normalFont));

            document.close();
        }
    }

    private Image buildQrImage(String payload) throws DocumentException {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 220, 220, hints);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            Image qrImage = Image.getInstance(bufferedImage, null);
            qrImage.scaleAbsolute(120, 120);
            qrImage.setAlignment(Element.ALIGN_LEFT);
            return qrImage;
        } catch (WriterException | IOException exception) {
            throw new DocumentException(exception);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new Phrase(text, font));
    }
}
