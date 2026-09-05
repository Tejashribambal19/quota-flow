package com.tejashri.quota.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillingReportResponse(
        String invoiceNumber,
        UUID tenantId,
        String tenantName,
        String planName,
        String billingMonth,
        BigDecimal baseAmount,
        BigDecimal utilizedValue,
        BigDecimal totalPayable,
        String status,
        List<UsageSummaryResponse.ResourceUsage> usageDetails
) {
}