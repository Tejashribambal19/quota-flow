package com.tejashri.quota.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejashri.quota.domain.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySlugIgnoreCase(String slug);
}