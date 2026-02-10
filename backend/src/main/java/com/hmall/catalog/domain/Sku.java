package com.hmall.catalog.domain;

import java.util.List;
import java.util.Objects;

/**
 * SKU 实体。每个必填维度选一 Option，由 specOptionIds 表示。
 */
public class Sku {

    private final Long id;
    private final Long spuId;
    private final String displayName;
    private final long priceCents;
    private final List<Long> specOptionIds;

    /** 新建（尚未持久化，id 为 null） */
    public Sku(Long spuId, String displayName, long priceCents, List<Long> specOptionIds) {
        this.id = null;
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.displayName = displayName;
        this.priceCents = priceCents;
        this.specOptionIds = specOptionIds != null ? List.copyOf(specOptionIds) : List.of();
    }

    /** 从持久化还原 */
    public Sku(Long id, Long spuId, String displayName, long priceCents, List<Long> specOptionIds) {
        this.id = Objects.requireNonNull(id, "id");
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.displayName = displayName;
        this.priceCents = priceCents;
        this.specOptionIds = specOptionIds != null ? List.copyOf(specOptionIds) : List.of();
    }

    public Long getId() {
        return id;
    }

    public Long getSpuId() {
        return spuId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public List<Long> getSpecOptionIds() {
        return specOptionIds;
    }
}
