package com.tejashri.quota.dto;

import com.tejashri.quota.domain.ResourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConsumeUsageRequest(

        @NotNull
        ResourceType resourceType,

        @Positive
        long quantity,

        @NotBlank
        @Size(max = 100)
        String requestId

) {
}