package com.tejashri.quota.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tejashri.quota.domain.QuotaLevel;
import com.tejashri.quota.domain.ResourceType;

public record UsageSummaryResponse(
        UUID tenantId,
        String tenantName,
        String planName,
        String billingMonth,
        BigDecimal monthlyPrice,
        List<ResourceUsage> resources
) {

    public record ResourceUsage(
            ResourceType resourceType,
            long used,
            long limit,
            double percentage,
            QuotaLevel level,
            BigDecimal allocatedCost,
            BigDecimal consumedCost
    ) {
    }
}