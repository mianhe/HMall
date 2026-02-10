package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * 规格维度实体。同 SPU 内名称唯一；同 SPU 下至多一个 affectsAppearance=true。
 */
public class SpecDimension {

    private final Long id;
    private final Long spuId;
    private final String name;
    private final boolean required;
    private final Integer sortOrder;
    private final boolean affectsAppearance;

    /** 新建（尚未持久化，id 为 null） */
    public SpecDimension(Long spuId, String name, boolean required, Integer sortOrder, boolean affectsAppearance) {
        this.id = null;
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.name = Objects.requireNonNull(name, "name");
        this.required = required;
        this.sortOrder = sortOrder;
        this.affectsAppearance = affectsAppearance;
    }

    /** 从持久化还原 */
    public SpecDimension(Long id, Long spuId, String name, boolean required, Integer sortOrder, boolean affectsAppearance) {
        this.id = Objects.requireNonNull(id, "id");
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.name = Objects.requireNonNull(name, "name");
        this.required = required;
        this.sortOrder = sortOrder;
        this.affectsAppearance = affectsAppearance;
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

    public boolean isAffectsAppearance() {
        return affectsAppearance;
    }
}
