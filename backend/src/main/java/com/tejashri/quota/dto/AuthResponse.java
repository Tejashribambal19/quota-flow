package com.tejashri.quota.dto;

import java.util.UUID;

import com.tejashri.quota.domain.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        String fullName,
        String email,
        UserRole role,
        UUID tenantId
) {
}