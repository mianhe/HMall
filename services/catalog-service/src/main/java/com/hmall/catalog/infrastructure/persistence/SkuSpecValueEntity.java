package com.hmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "sku_spec_value", indexes = {
    @Index(name = "idx_sku_spec_value_sku_id", columnList = "sku_id"),
    @Index(name = "idx_sku_spec_value_option_id", columnList = "spec_option_id")
})
public class SkuSpecValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "spec_option_id", nullable = false)
    private Long specOptionId;

    public SkuSpecValueEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSpecOptionId() {
        return specOptionId;
    }

    public void setSpecOptionId(Long specOptionId) {
        this.specOptionId = specOptionId;
    }
}
