package com.tejashri.quota.service;

import com.tejashri.quota.domain.AppUser;
import com.tejashri.quota.domain.UserRole;
import com.tejashri.quota.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private final AppUserRepository userRepository;

    public void verifyAccess(UUID requestedTenantId) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        AppUser user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("User not found")
                );

        if (user.getRole() == UserRole.PLATFORM_ADMIN) {
            return;
        }

        if (user.getTenant() == null ||
                !user.getTenant().getId()
                        .equals(requestedTenantId)) {
            throw new AccessDeniedException(
                    "You cannot access another tenant's data"
            );
        }
    }
}