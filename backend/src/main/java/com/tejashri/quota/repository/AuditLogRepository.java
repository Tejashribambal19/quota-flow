package com.tejashri.quota.repository;

import com.tejashri.quota.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findTopByOrderByOccurredAtDesc();

    List<AuditLog> findAllByOrderByOccurredAtAsc();
}