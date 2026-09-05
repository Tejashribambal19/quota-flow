package com.tejashri.quota.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "Slug can contain lowercase letters, numbers and hyphens only"
        )
        String slug,

        @NotNull
        UUID planId,

        @Min(1)
        @Max(28)
        int billingCycleDay

) {
}