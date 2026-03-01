package com.hmall.cart.application;

import java.math.BigDecimal;
import java.util.List;

public record AvailableServiceView(
    Long serviceSpuId,
    String name,
    List<AvailableServiceSkuView> bindings
) {
    public record AvailableServiceSkuView(Long bindingId, Long serviceSkuId, BigDecimal price) {}
}
