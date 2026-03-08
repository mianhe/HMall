package com.hmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "spec_option", indexes = {
    @Index(name = "idx_spec_option_dimension_id", columnList = "spec_dimension_id")
})
public class SpecOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spec_dimension_id", nullable = false)
    private Long specDimensionId;

    @Column(name = "option_value", nullable = false)
    private String optionValue;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public SpecOptionEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpecDimensionId() {
        return specDimensionId;
    }

    public void setSpecDimensionId(Long specDimensionId) {
        this.specDimensionId = specDimensionId;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
