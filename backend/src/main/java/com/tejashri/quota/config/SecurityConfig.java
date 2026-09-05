package com.tejashri.quota.config;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tejashri.quota.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationProvider(authenticationProvider())

                .exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            String body = """
                                    {
                                      "timestamp": "%s",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "Authentication is required or the token is invalid",
                                      "path": "%s"
                                    }
                                    """.formatted(
                                    Instant.now(),
                                    request.getRequestURI()
                            );

                            response.getWriter().write(body);
                        })

                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            String body = """
                                    {
                                      "timestamp": "%s",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "You do not have permission to access this resource",
                                      "path": "%s"
                                    }
                                    """.formatted(
                                    Instant.now(),
                                    request.getRequestURI()
                            );

                            response.getWriter().write(body);
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()

                        // Registration and login do not require JWT.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Only platform administrators can create/update plans.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/plans/**"
                        ).hasRole("PLATFORM_ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/plans/**"
                        ).hasRole("PLATFORM_ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/plans/**"
                        ).hasRole("PLATFORM_ADMIN")

                        // Authenticated users can read plans.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/plans/**"
                        ).authenticated()

                        // Only platform administrators manage tenants.
                        .requestMatchers(
                                "/api/tenants/**"
                        ).hasRole("PLATFORM_ADMIN")

                        // Usage and reports require authentication.
                        .requestMatchers(
                                "/api/usage/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/reports/**"
                        ).authenticated()

                        // Only platform administrators inspect audit logs.
                        .requestMatchers(
                                "/api/audit/**"
                        ).hasRole("PLATFORM_ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}