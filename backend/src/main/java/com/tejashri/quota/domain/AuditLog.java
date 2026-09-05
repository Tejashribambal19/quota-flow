package com.tejashri.quota.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;
}