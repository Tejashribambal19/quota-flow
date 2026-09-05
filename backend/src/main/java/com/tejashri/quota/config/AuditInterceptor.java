package com.tejashri.quota.config;

import com.tejashri.quota.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditService auditService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = "anonymous";
        String role = "ANONYMOUS";

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(
                        authentication.getPrincipal()
                )) {

            email = authentication.getName();

            role = authentication.getAuthorities()
                    .stream()
                    .findFirst()
                    .map(authority ->
                            authority.getAuthority()
                    )
                    .orElse("UNKNOWN");
        }

        try {
            auditService.record(
                    email,
                    role,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus()
            );
        } catch (RuntimeException exceptionIgnored) {
            // Audit failure must not replace the original API response.
        }
    }
}