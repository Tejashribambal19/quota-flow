package com.tejashri.quota.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejashri.quota.dto.BillingReportResponse;
import com.tejashri.quota.dto.UsageSummaryResponse;
import com.tejashri.quota.service.InvoicePdfService;
import com.tejashri.quota.service.ReportingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports/tenants")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final InvoicePdfService invoicePdfService;

    @GetMapping("/{tenantId}/usage")
    public ResponseEntity<UsageSummaryResponse> usageSummary(
            @PathVariable UUID tenantId
    ) {
        return ResponseEntity.ok(
                reportingService.getUsageSummary(tenantId)
        );
    }

    @GetMapping("/{tenantId}/billing")
    public ResponseEntity<BillingReportResponse> billingReport(
            @PathVariable UUID tenantId
    ) {
        return ResponseEntity.ok(
                reportingService.getBillingReport(tenantId)
        );
    }

    @GetMapping(
            value = "/{tenantId}/invoice.pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable UUID tenantId
    ) {
        byte[] pdf = invoicePdfService.generateInvoice(tenantId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quota-invoice-"
                                + tenantId
                                + ".pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}