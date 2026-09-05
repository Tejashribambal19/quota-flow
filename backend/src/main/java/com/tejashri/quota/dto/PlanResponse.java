package com.tejashri.quota.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tejashri.quota.domain.ResourceType;

public record PlanResponse(
        UUID id,
        String name,
        String description,
        BigDecimal monthlyPrice,
        boolean active,
        List<QuotaResponse> quotas
        ) {

    public record QuotaResponse(
            UUID id,
            ResourceType resourceType,
            long hardLimit,
            int warningPercentage,
            int criticalPercentage
            ) {

    }
}
