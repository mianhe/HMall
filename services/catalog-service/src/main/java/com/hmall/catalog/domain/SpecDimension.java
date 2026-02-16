package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * 规格维度实体。同 SPU 内名称唯一。
 */
public class SpecDimension {

    private final Long id;
    private final Long spuId;
    private final String name;
    private final boolean required;
    private final Integer sortOrder;

    /** 新建（尚未持久化，id 为 null） */
    public SpecDimension(Long spuId, String name, boolean required, Integer sortOrder) {
        this.id = null;
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.name = Objects.requireNonNull(name, "name");
        this.required = required;
        this.sortOrder = sortOrder;
    }

    /** 从持久化还原 */
    public SpecDimension(Long id, Long spuId, String name, boolean required, Integer sortOrder) {
        this.id = Objects.requireNonNull(id, "id");
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.name = Objects.requireNonNull(name, "name");
        this.required = required;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getSpuId() {
        return spuId;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
