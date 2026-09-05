package com.tejashri.quota.service;

import com.tejashri.quota.domain.PlanQuota;
import com.tejashri.quota.domain.QuotaLevel;
import com.tejashri.quota.domain.ResourceType;
import com.tejashri.quota.domain.Tenant;
import com.tejashri.quota.dto.BillingReportResponse;
import com.tejashri.quota.dto.UsageSummaryResponse;
import com.tejashri.quota.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TenantRepository tenantRepository;
    private final StringRedisTemplate redisTemplate;
    private final TenantAccessService tenantAccessService;

    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(UUID tenantId) {
        tenantAccessService.verifyAccess(tenantId);
        Tenant tenant = getTenant(tenantId);
        YearMonth month = YearMonth.now(ZoneOffset.UTC);

        List<PlanQuota> quotas = tenant.getPlan().getQuotas();

        BigDecimal allocatedCost = quotas.isEmpty()
                ? BigDecimal.ZERO
                : tenant.getPlan()
                        .getMonthlyPrice()
                        .divide(
                                BigDecimal.valueOf(quotas.size()),
                                2,
                                RoundingMode.HALF_UP
                        );

        List<UsageSummaryResponse.ResourceUsage> resources
                = new ArrayList<>();

        for (PlanQuota quota : quotas) {
            long used = getCurrentUsage(
                    tenantId,
                    month,
                    quota.getResourceType()
            );

            double percentage = quota.getHardLimit() == 0
                    ? 100
                    : used * 100.0 / quota.getHardLimit();

            percentage
                    = Math.round(percentage * 100.0) / 100.0;

            QuotaLevel level = determineLevel(
                    percentage,
                    quota
            );

            BigDecimal consumedCost = allocatedCost
                    .multiply(BigDecimal.valueOf(
                            Math.min(percentage, 100)
                    ))
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );

            resources.add(
                    new UsageSummaryResponse.ResourceUsage(
                            quota.getResourceType(),
                            used,
                            quota.getHardLimit(),
                            percentage,
                            level,
                            allocatedCost,
                            consumedCost
                    )
            );
        }

        return new UsageSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getPlan().getName(),
                month.toString(),
                tenant.getPlan().getMonthlyPrice(),
                resources
        );
    }

    @Transactional(readOnly = true)
    public BillingReportResponse getBillingReport(UUID tenantId) {
        UsageSummaryResponse summary
                = getUsageSummary(tenantId);

        BigDecimal utilizedValue = summary.resources()
                .stream()
                .map(
                        UsageSummaryResponse.ResourceUsage::consumedCost
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String invoiceNumber = "INV-"
                + summary.billingMonth().replace("-", "")
                + "-"
                + tenantId.toString()
                        .substring(0, 8)
                        .toUpperCase();

        return new BillingReportResponse(
                invoiceNumber,
                summary.tenantId(),
                summary.tenantName(),
                summary.planName(),
                summary.billingMonth(),
                summary.monthlyPrice(),
                utilizedValue,
                summary.monthlyPrice(),
                "CURRENT",
                summary.resources()
        );
    }

    private Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Tenant not found"
                )
                );
    }

    private long getCurrentUsage(
            UUID tenantId,
            YearMonth month,
            ResourceType resourceType
    ) {
        String redisKey = "quota:"
                + tenantId
                + ":"
                + month
                + ":"
                + resourceType.name();

        String value = redisTemplate.opsForValue()
                .get(redisKey);

        return value == null ? 0 : Long.parseLong(value);
    }

    private QuotaLevel determineLevel(
            double percentage,
            PlanQuota quota
    ) {
        if (percentage >= 100) {
            return QuotaLevel.BLOCKED;
        }

        if (percentage >= quota.getCriticalPercentage()) {
            return QuotaLevel.CRITICAL;
        }

        if (percentage >= quota.getWarningPercentage()) {
            return QuotaLevel.WARNING;
        }

        return QuotaLevel.NORMAL;
    }
}
