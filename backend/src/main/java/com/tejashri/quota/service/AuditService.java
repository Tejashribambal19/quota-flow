package com.tejashri.quota.service;

import com.tejashri.quota.domain.AuditLog;
import com.tejashri.quota.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String GENESIS_HASH =
            "0".repeat(64);

    private final AuditLogRepository auditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void record(
            String actorEmail,
            String actorRole,
            String method,
            String path,
            int statusCode
    ) {
        String previousHash = auditRepository
                .findTopByOrderByOccurredAtDesc()
                .map(AuditLog::getHash)
                .orElse(GENESIS_HASH);

        UUID id = UUID.randomUUID();
       Instant occurredAt = Instant.now()
        .truncatedTo(ChronoUnit.MICROS);

        String hash = calculateHash(
                id,
                actorEmail,
                actorRole,
                method,
                path,
                statusCode,
                occurredAt,
                previousHash
        );

        auditRepository.save(
                AuditLog.builder()
                        .id(id)
                        .actorEmail(actorEmail)
                        .actorRole(actorRole)
                        .httpMethod(method)
                        .requestPath(path)
                        .statusCode(statusCode)
                        .occurredAt(occurredAt)
                        .previousHash(previousHash)
                        .hash(hash)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogs() {
        return auditRepository
                .findAllByOrderByOccurredAtAsc();
    }

    @Transactional(readOnly = true)
    public boolean verifyChain() {
        List<AuditLog> logs = getLogs();
        String expectedPreviousHash = GENESIS_HASH;

        for (AuditLog log : logs) {
            if (!log.getPreviousHash()
                    .equals(expectedPreviousHash)) {
                return false;
            }

            String expectedHash = calculateHash(
                    log.getId(),
                    log.getActorEmail(),
                    log.getActorRole(),
                    log.getHttpMethod(),
                    log.getRequestPath(),
                    log.getStatusCode(),
                    log.getOccurredAt(),
                    log.getPreviousHash()
            );

            if (!log.getHash().equals(expectedHash)) {
                return false;
            }

            expectedPreviousHash = log.getHash();
        }

        return true;
    }

    private String calculateHash(
            UUID id,
            String actorEmail,
            String actorRole,
            String method,
            String path,
            int statusCode,
            Instant occurredAt,
            String previousHash
    ) {
        String content = String.join(
                "|",
                id.toString(),
                actorEmail,
                actorRole,
                method,
                path,
                String.valueOf(statusCode),
                occurredAt.toString(),
                previousHash
        );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    content.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}