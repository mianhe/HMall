package com.hmall.catalog.domain;

import java.util.Objects;

public class ServiceBinding {
    private final Long id;
    private final Long serviceSkuId;
    private final Long targetSpuId;
    private Long priceCents;

    /** 新建 */
    public ServiceBinding(Long serviceSkuId, Long targetSpuId, Long priceCents) {
        this.id = null;
        this.serviceSkuId = Objects.requireNonNull(serviceSkuId, "serviceSkuId");
        this.targetSpuId = Objects.requireNonNull(targetSpuId, "targetSpuId");
        validatePriceCents(priceCents);
        this.priceCents = priceCents;
    }

    /** 从持久化还原 */
    public ServiceBinding(Long id, Long serviceSkuId, Long targetSpuId, Long priceCents) {
        this.id = Objects.requireNonNull(id, "id");
        this.serviceSkuId = Objects.requireNonNull(serviceSkuId, "serviceSkuId");
        this.targetSpuId = Objects.requireNonNull(targetSpuId, "targetSpuId");
        this.priceCents = priceCents;
    }

    public void updatePrice(Long priceCents) {
        validatePriceCents(priceCents);
        this.priceCents = priceCents;
    }

    public Long getId() { return id; }
    public Long getServiceSkuId() { return serviceSkuId; }
    public Long getTargetSpuId() { return targetSpuId; }
    public Long getPriceCents() { return priceCents; }

    private static void validatePriceCents(Long priceCents) {
        if (priceCents != null && priceCents < 0) throw new IllegalArgumentException("priceCents must be >= 0");
    }
}
