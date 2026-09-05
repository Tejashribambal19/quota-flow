package com.tejashri.quota.dto;

import java.util.UUID;

import com.tejashri.quota.domain.QuotaLevel;
import com.tejashri.quota.domain.ResourceType;

public record UsageDecisionResponse(
        UUID tenantId,
        ResourceType resourceType,
        boolean allowed,
        long requestedQuantity,
        long used,
        long limit,
        double percentage,
        QuotaLevel level,
        String message
) {
}