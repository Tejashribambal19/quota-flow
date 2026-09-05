package com.tejashri.quota.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "usage_events",
        indexes = {
                @Index(
                        name = "idx_usage_tenant_created",
                        columnList = "tenant_id, created_at"
                )
        }
)
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(nullable = false)
    private boolean accepted;

    @Column(name = "used_after", nullable = false)
    private long usedAfter;

    @Column(name = "quota_limit", nullable = false)
    private long quotaLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_level", nullable = false)
    private QuotaLevel quotaLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UsageEvent() {
    }

    public UsageEvent(
            UUID id,
            Tenant tenant,
            ResourceType resourceType,
            long quantity,
            String requestId,
            boolean accepted,
            long usedAfter,
            long quotaLimit,
            QuotaLevel quotaLevel,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.tenant = tenant;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.requestId = requestId;
        this.accepted = accepted;
        this.usedAfter = usedAfter;
        this.quotaLimit = quotaLimit;
        this.quotaLevel = quotaLevel;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public long getUsedAfter() {
        return usedAfter;
    }

    public void setUsedAfter(long usedAfter) {
        this.usedAfter = usedAfter;
    }

    public long getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public QuotaLevel getQuotaLevel() {
        return quotaLevel;
    }

    public void setQuotaLevel(QuotaLevel quotaLevel) {
        this.quotaLevel = quotaLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {

        private UUID id;
        private Tenant tenant;
        private ResourceType resourceType;
        private long quantity;
        private String requestId;
        private boolean accepted;
        private long usedAfter;
        private long quotaLimit;
        private QuotaLevel quotaLevel;
        private LocalDateTime createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder tenant(Tenant tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder quantity(long quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder accepted(boolean accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder usedAfter(long usedAfter) {
            this.usedAfter = usedAfter;
            return this;
        }

        public Builder quotaLimit(long quotaLimit) {
            this.quotaLimit = quotaLimit;
            return this;
        }

        public Builder quotaLevel(QuotaLevel quotaLevel) {
            this.quotaLevel = quotaLevel;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UsageEvent build() {
            return new UsageEvent(
                    id,
                    tenant,
                    resourceType,
                    quantity,
                    requestId,
                    accepted,
                    usedAfter,
                    quotaLimit,
                    quotaLevel,
                    createdAt
            );
        }
    }
}