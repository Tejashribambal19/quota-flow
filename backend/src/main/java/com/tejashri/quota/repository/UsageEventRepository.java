package com.tejashri.quota.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejashri.quota.domain.UsageEvent;

public interface UsageEventRepository
        extends JpaRepository<UsageEvent, UUID> {

    Optional<UsageEvent> findByRequestId(String requestId);

    List<UsageEvent> findTop100ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
