package com.tejashri.quota.dto;

import java.util.UUID;

import com.tejashri.quota.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotNull
        UserRole role,

        UUID tenantId

) {
}