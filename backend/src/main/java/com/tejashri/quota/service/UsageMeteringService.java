package com.tejashri.quota.service;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejashri.quota.domain.PlanQuota;
import com.tejashri.quota.domain.QuotaLevel;
import com.tejashri.quota.domain.ResourceType;
import com.tejashri.quota.domain.Tenant;
import com.tejashri.quota.domain.TenantStatus;
import com.tejashri.quota.domain.UsageEvent;
import com.tejashri.quota.dto.ConsumeUsageRequest;
import com.tejashri.quota.dto.UsageDecisionResponse;
import com.tejashri.quota.dto.UsageEventResponse;
import com.tejashri.quota.repository.PlanQuotaRepository;
import com.tejashri.quota.repository.TenantRepository;
import com.tejashri.quota.repository.UsageEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsageMeteringService {

    private static final String QUOTA_SCRIPT = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local amount = tonumber(ARGV[1])
            local quotaLimit = tonumber(ARGV[2])
            local nextValue = current + amount

            if nextValue > quotaLimit then
                return -(current + 1)
            end

            local updated = redis.call('INCRBY', KEYS[1], amount)
            redis.call('EXPIRE', KEYS[1], 3456000)
            return updated
            """;

    private final TenantRepository tenantRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UsageEventRepository usageEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final TenantAccessService tenantAccessService;

    @Transactional(readOnly = true)
    public List<UsageEventResponse> getRecentEvents(UUID tenantId) {
        tenantAccessService.verifyAccess(tenantId);
        return usageEventRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(event -> new UsageEventResponse(
                        event.getId(), event.getResourceType(), event.getQuantity(), event.getRequestId(),
                        event.isAccepted(), event.getUsedAfter(), event.getQuotaLimit(),
                        event.getQuotaLevel(), event.getCreatedAt()))
                .toList();
    }

    @Transactional
    public UsageDecisionResponse consume(
            UUID tenantId,
            ConsumeUsageRequest request
    ) {
        tenantAccessService.verifyAccess(tenantId);
        
        UsageEvent existingEvent = usageEventRepository
                .findByRequestId(request.requestId())
                .orElse(null);

        if (existingEvent != null) {
            return convertExistingEvent(existingEvent);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Tenant not found")
                );

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException("Tenant is not active");
        }

        PlanQuota quota = planQuotaRepository
                .findByPlanIdAndResourceType(
                        tenant.getPlan().getId(),
                        request.resourceType()
                )
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "No quota configured for this resource"
                )
                );

        String redisKey = createRedisKey(
                tenantId,
                request.resourceType()
        );

        DefaultRedisScript<Long> script
                = new DefaultRedisScript<>(QUOTA_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(redisKey),
                String.valueOf(request.quantity()),
                String.valueOf(quota.getHardLimit())
        );

        if (result == null) {
            throw new IllegalStateException(
                    "Redis could not process the usage request"
            );
        }

        boolean allowed = result >= 0;
        long used = allowed ? result : (-result - 1);

        QuotaLevel level = calculateLevel(
                allowed,
                used,
                quota
        );

        UsageEvent event = UsageEvent.builder()
                .tenant(tenant)
                .resourceType(request.resourceType())
                .quantity(request.quantity())
                .requestId(request.requestId())
                .accepted(allowed)
                .usedAfter(used)
                .quotaLimit(quota.getHardLimit())
                .quotaLevel(level)
                .build();

        usageEventRepository.save(event);

        return createResponse(
                tenantId,
                request,
                allowed,
                used,
                quota.getHardLimit(),
                level
        );
    }

    private QuotaLevel calculateLevel(
            boolean allowed,
            long used,
            PlanQuota quota
    ) {
        if (!allowed) {
            return QuotaLevel.BLOCKED;
        }

        double percentage
                = used * 100.0 / quota.getHardLimit();

        if (percentage >= quota.getCriticalPercentage()) {
            return QuotaLevel.CRITICAL;
        }

        if (percentage >= quota.getWarningPercentage()) {
            return QuotaLevel.WARNING;
        }

        return QuotaLevel.NORMAL;
    }

    private UsageDecisionResponse createResponse(
            UUID tenantId,
            ConsumeUsageRequest request,
            boolean allowed,
            long used,
            long limit,
            QuotaLevel level
    ) {
        double percentage = limit == 0
                ? 100
                : used * 100.0 / limit;

        String message = switch (level) {
            case NORMAL ->
                "Usage accepted";
            case WARNING ->
                "Warning: quota usage has crossed 80%";
            case CRITICAL ->
                "Critical: quota usage has crossed 90%";
            case BLOCKED ->
                "Request blocked: quota limit exceeded";
        };

        return new UsageDecisionResponse(
                tenantId,
                request.resourceType(),
                allowed,
                request.quantity(),
                used,
                limit,
                Math.round(percentage * 100.0) / 100.0,
                level,
                message
        );
    }

    private UsageDecisionResponse convertExistingEvent(
            UsageEvent event
    ) {
        double percentage = event.getQuotaLimit() == 0
                ? 100
                : event.getUsedAfter() * 100.0
                / event.getQuotaLimit();

        return new UsageDecisionResponse(
                event.getTenant().getId(),
                event.getResourceType(),
                event.isAccepted(),
                event.getQuantity(),
                event.getUsedAfter(),
                event.getQuotaLimit(),
                Math.round(percentage * 100.0) / 100.0,
                event.getQuotaLevel(),
                "Duplicate request ignored"
        );
    }

    private String createRedisKey(
            UUID tenantId,
            ResourceType resourceType
    ) {
        String month = YearMonth.now(ZoneOffset.UTC).toString();

        return "quota:"
                + tenantId
                + ":"
                + month
                + ":"
                + resourceType.name();
    }
}
