package com.tejashri.quota.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.tejashri.quota.dto.BillingReportResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final ReportingService reportingService;

    public byte[] generateInvoice(
            java.util.UUID tenantId
    ) {
        BillingReportResponse report =
                reportingService.getBillingReport(tenantId);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font normal = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

            PDType1Font bold = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float y = 750;

                writeLine(
                        content,
                        bold,
                        22,
                        50,
                        y,
                        "QUOTA FLOW"
                );

                y -= 38;

                writeLine(
                        content,
                        bold,
                        18,
                        50,
                        y,
                        "Usage and Billing Invoice"
                );

                y -= 45;

                writeLine(content, normal, 12, 50, y,
                        "Invoice: " + report.invoiceNumber());

                y -= 22;

                writeLine(content, normal, 12, 50, y,
                        "Tenant: " + report.tenantName());

                y -= 22;

                writeLine(content, normal, 12, 50, y,
                        "Plan: " + report.planName());

                y -= 22;

                writeLine(content, normal, 12, 50, y,
                        "Billing month: " + report.billingMonth());

                y -= 35;

                writeLine(
                        content,
                        bold,
                        14,
                        50,
                        y,
                        "Resource usage"
                );

                y -= 28;

                for (var usage : report.usageDetails()) {
                    String line =
                            usage.resourceType().name().replace("_", " ")
                                    + "    "
                                    + usage.used()
                                    + " / "
                                    + usage.limit()
                                    + "    "
                                    + usage.percentage()
                                    + "%    "
                                    + usage.level();

                    writeLine(
                            content,
                            normal,
                            11,
                            50,
                            y,
                            line
                    );

                    y -= 24;
                }

                y -= 18;

                writeLine(content, bold, 13, 50, y,
                        "Base amount: INR " + report.baseAmount());

                y -= 24;

                writeLine(content, bold, 13, 50, y,
                        "Utilized value: INR " + report.utilizedValue());

                y -= 24;

                writeLine(content, bold, 15, 50, y,
                        "Total payable: INR " + report.totalPayable());

                y -= 28;

                writeLine(content, normal, 11, 50, y,
                        "Status: " + report.status());
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not generate invoice PDF",
                    exception
            );
        }
    }

    private void writeLine(
            PDPageContentStream content,
            PDType1Font font,
            float size,
            float x,
            float y,
            String text
    ) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}