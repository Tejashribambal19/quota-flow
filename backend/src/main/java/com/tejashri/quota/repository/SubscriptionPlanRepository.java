package com.tejashri.quota.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejashri.quota.domain.SubscriptionPlan;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, UUID> {

    boolean existsByNameIgnoreCase(String name);
}