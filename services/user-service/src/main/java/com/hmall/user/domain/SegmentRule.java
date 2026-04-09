package com.hmall.user.domain;

import com.hmall.user.application.UserBadRequestException;

import java.time.Instant;
import java.util.Objects;

public class SegmentRule {
    private final Long id;
    private final String name;
    private final SegmentCondition conditions;
    private final SegmentRuleStatus status;
    private final Long lastPreviewCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SegmentRule(String name, SegmentCondition conditions) {
        this(null, name, conditions, SegmentRuleStatus.DRAFT, null, Instant.now(), Instant.now());
    }

    public SegmentRule(
        Long id,
        String name,
        SegmentCondition conditions,
        SegmentRuleStatus status,
        Long lastPreviewCount,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.name = normalizeName(name);
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.status = status == null ? SegmentRuleStatus.DRAFT : status;
        this.lastPreviewCount = lastPreviewCount;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public SegmentRule withPreviewCount(long hitCount) {
        return new SegmentRule(id, name, conditions, status, hitCount, createdAt, Instant.now());
    }

    public SegmentRule activate() {
        if (lastPreviewCount == null || lastPreviewCount <= 0) {
            throw new UserBadRequestException("命中人数为 0，规则不可激活");
        }
        return new SegmentRule(id, name, conditions, SegmentRuleStatus.ACTIVE, lastPreviewCount, createdAt, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SegmentCondition getConditions() {
        return conditions;
    }

    public SegmentRuleStatus getStatus() {
        return status;
    }

    public Long getLastPreviewCount() {
        return lastPreviewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new UserBadRequestException("规则名称不能为空");
        }
        return name.trim();
    }
}
