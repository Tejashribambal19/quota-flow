package com.tejashri.quota.dto;

import com.tejashri.quota.domain.ResourceType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlanQuotaRequest(

        @NotNull
        ResourceType resourceType,

        @Positive
        long hardLimit,

        @Min(1)
        @Max(99)
        int warningPercentage,

        @Min(1)
        @Max(99)
        int criticalPercentage

) {
}