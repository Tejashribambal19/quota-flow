package com.tejashri.quota.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal monthlyPrice,

        @NotEmpty
        List<@Valid PlanQuotaRequest> quotas

) {
}