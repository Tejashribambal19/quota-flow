package com.tejashri.quota.dto;

import java.util.UUID;

import com.tejashri.quota.domain.TenantStatus;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        UUID planId,
        String planName,
        int billingCycleDay
) {
}