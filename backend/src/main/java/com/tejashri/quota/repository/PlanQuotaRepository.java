package com.tejashri.quota.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejashri.quota.domain.PlanQuota;
import com.tejashri.quota.domain.ResourceType;

public interface PlanQuotaRepository
        extends JpaRepository<PlanQuota, UUID> {

    Optional<PlanQuota> findByPlanIdAndResourceType(
            UUID planId,
            ResourceType resourceType
    );
}