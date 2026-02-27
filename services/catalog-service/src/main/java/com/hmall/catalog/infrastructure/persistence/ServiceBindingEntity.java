package com.hmall.catalog.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "service_binding")
public class ServiceBindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_sku_id", nullable = false)
    private Long serviceSkuId;

    @Column(name = "target_spu_id", nullable = false)
    private Long targetSpuId;

    @Column(name = "price_cents")
    private Long priceCents;

    public ServiceBindingEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getServiceSkuId() { return serviceSkuId; }
    public void setServiceSkuId(Long serviceSkuId) { this.serviceSkuId = serviceSkuId; }
    public Long getTargetSpuId() { return targetSpuId; }
    public void setTargetSpuId(Long targetSpuId) { this.targetSpuId = targetSpuId; }
    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }
}
