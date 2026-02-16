package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * 规格选项实体。同维度内 optionValue 唯一。
 */
public class SpecOption {

    private final Long id;
    private final Long specDimensionId;
    private final String optionValue;
    private final Integer sortOrder;

    /** 新建（尚未持久化，id 为 null） */
    public SpecOption(Long specDimensionId, String optionValue, Integer sortOrder) {
        this.id = null;
        this.specDimensionId = Objects.requireNonNull(specDimensionId, "specDimensionId");
        this.optionValue = Objects.requireNonNull(optionValue, "optionValue");
        this.sortOrder = sortOrder;
    }

    /** 从持久化还原 */
    public SpecOption(Long id, Long specDimensionId, String optionValue, Integer sortOrder) {
        this.id = Objects.requireNonNull(id, "id");
        this.specDimensionId = Objects.requireNonNull(specDimensionId, "specDimensionId");
        this.optionValue = Objects.requireNonNull(optionValue, "optionValue");
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getSpecDimensionId() {
        return specDimensionId;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
