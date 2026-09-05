package com.tejashri.quota.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejashri.quota.domain.AppUser;
import com.tejashri.quota.domain.Tenant;
import com.tejashri.quota.domain.UserRole;
import com.tejashri.quota.dto.AuthResponse;
import com.tejashri.quota.dto.LoginRequest;
import com.tejashri.quota.dto.RegisterRequest;
import com.tejashri.quota.repository.AppUserRepository;
import com.tejashri.quota.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Tenant tenant = resolveTenant(request);

        AppUser user = AppUser.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .tenant(tenant)
                .active(true)
                .build();

        AppUser savedUser = userRepository.save(user);
        return createResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );

        AppUser user = userRepository
                .findByEmailIgnoreCase(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        return createResponse(user);
    }

    private Tenant resolveTenant(RegisterRequest request) {
        if (request.role() == UserRole.PLATFORM_ADMIN) {
            if (userRepository.existsByRole(UserRole.PLATFORM_ADMIN)) {
                throw new IllegalArgumentException(
                        "A platform administrator already exists"
                );
            }

            return null;
        }

        if (request.tenantId() == null) {
            throw new IllegalArgumentException(
                    "Tenant ID is required for a tenant administrator"
            );
        }

        return tenantRepository.findById(request.tenantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found")
                );
    }

    private AuthResponse createResponse(AppUser user) {
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getTenant() == null
                        ? null
                        : user.getTenant().getId()
        );
    }
}