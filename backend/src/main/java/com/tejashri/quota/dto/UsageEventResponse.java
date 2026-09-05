package com.tejashri.quota.dto;

import com.tejashri.quota.domain.QuotaLevel;
import com.tejashri.quota.domain.ResourceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsageEventResponse(
        UUID id,
        ResourceType resourceType,
        long quantity,
        String requestId,
        boolean accepted,
        long usedAfter,
        long quotaLimit,
        QuotaLevel quotaLevel,
        LocalDateTime createdAt
) {
}
