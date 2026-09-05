package com.tejashri.quota.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejashri.quota.domain.SubscriptionPlan;
import com.tejashri.quota.domain.Tenant;
import com.tejashri.quota.domain.TenantStatus;
import com.tejashri.quota.dto.CreateTenantRequest;
import com.tejashri.quota.dto.TenantResponse;
import com.tejashri.quota.repository.SubscriptionPlanRepository;
import com.tejashri.quota.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new IllegalArgumentException(
                    "A tenant with this slug already exists"
            );
        }

        SubscriptionPlan plan = planRepository.findById(request.planId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Subscription plan not found")
                );

        if (!plan.isActive()) {
            throw new IllegalArgumentException(
                    "The selected subscription plan is inactive"
            );
        }

        Tenant tenant = Tenant.builder()
                .name(request.name().trim())
                .slug(request.slug().trim().toLowerCase())
                .status(TenantStatus.ACTIVE)
                .plan(plan)
                .billingCycleDay(request.billingCycleDay())
                .build();

        return convertToResponse(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private TenantResponse convertToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getPlan().getId(),
                tenant.getPlan().getName(),
                tenant.getBillingCycleDay()
        );
    }
}