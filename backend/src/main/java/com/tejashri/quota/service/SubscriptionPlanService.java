package com.tejashri.quota.service;

import com.tejashri.quota.domain.PlanQuota;
import com.tejashri.quota.domain.ResourceType;
import com.tejashri.quota.domain.SubscriptionPlan;
import com.tejashri.quota.dto.CreatePlanRequest;
import com.tejashri.quota.dto.PlanQuotaRequest;
import com.tejashri.quota.dto.PlanResponse;
import com.tejashri.quota.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request) {
        if (planRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "A subscription plan with this name already exists"
            );
        }

        validateQuotas(request.quotas());

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.name().trim())
                .description(request.description())
                .monthlyPrice(request.monthlyPrice())
                .active(true)
                .build();

        for (PlanQuotaRequest quotaRequest : request.quotas()) {
            PlanQuota quota = PlanQuota.builder()
                    .resourceType(quotaRequest.resourceType())
                    .hardLimit(quotaRequest.hardLimit())
                    .warningPercentage(quotaRequest.warningPercentage())
                    .criticalPercentage(quotaRequest.criticalPercentage())
                    .build();

            plan.addQuota(quota);
        }

        SubscriptionPlan savedPlan = planRepository.save(plan);
        return convertToResponse(savedPlan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private void validateQuotas(List<PlanQuotaRequest> quotas) {
        Set<ResourceType> resourceTypes = new HashSet<>();

        for (PlanQuotaRequest quota : quotas) {
            if (quota.warningPercentage() >= quota.criticalPercentage()) {
                throw new IllegalArgumentException(
                        "Warning percentage must be lower than critical percentage"
                );
            }

            if (!resourceTypes.add(quota.resourceType())) {
                throw new IllegalArgumentException(
                        "Duplicate resource type: " + quota.resourceType()
                );
            }
        }
    }

    private PlanResponse convertToResponse(SubscriptionPlan plan) {
        List<PlanResponse.QuotaResponse> quotas = plan.getQuotas()
                .stream()
                .map(quota -> new PlanResponse.QuotaResponse(
                        quota.getId(),
                        quota.getResourceType(),
                        quota.getHardLimit(),
                        quota.getWarningPercentage(),
                        quota.getCriticalPercentage()
                ))
                .toList();

        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getMonthlyPrice(),
                plan.isActive(),
                quotas
        );
    }
}