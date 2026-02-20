package com.hmall.cart.acceptance;

import com.hmall.cart.application.port.SkuInfo;
import com.hmall.cart.application.port.SkuQueryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用 SKU 查询桩。测试步骤通过 register / unregister 控制 SKU 存在性与信息。
 */
public class StubSkuQueryPort implements SkuQueryPort {

    private final Map<Long, SkuInfo> skuStore = new ConcurrentHashMap<>();

    public void register(Long skuId, String name, BigDecimal price) {
        skuStore.put(skuId, new SkuInfo(skuId, name, price, null, true));
    }

    public void register(Long skuId) {
        register(skuId, "SKU-" + skuId, BigDecimal.valueOf(100));
    }

    public void markUnavailable(Long skuId) {
        SkuInfo existing = skuStore.get(skuId);
        if (existing != null) {
            skuStore.put(skuId, new SkuInfo(skuId, existing.name(), existing.price(), existing.imageUrl(), false));
        }
    }

    public void unregister(Long skuId) {
        skuStore.remove(skuId);
    }

    public void clear() {
        skuStore.clear();
    }

    @Override
    public boolean exists(Long skuId) {
        return skuStore.containsKey(skuId);
    }

    @Override
    public List<SkuInfo> queryByIds(List<Long> skuIds) {
        return skuIds.stream()
            .map(skuStore::get)
            .filter(info -> info != null)
            .toList();
    }

    @Override
    public Optional<SkuInfo> queryById(Long skuId) {
        return Optional.ofNullable(skuStore.get(skuId));
    }
}
