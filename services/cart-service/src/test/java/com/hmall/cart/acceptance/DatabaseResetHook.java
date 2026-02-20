package com.hmall.cart.acceptance;

import com.hmall.cart.infrastructure.persistence.CartJpaRepository;
import io.cucumber.java.Before;

/**
 * 每个场景执行前清空购物车表并重置测试上下文，避免场景间数据干扰。
 */
public class DatabaseResetHook {

    private final CartJpaRepository cartJpaRepository;
    private final CartTestContext context;
    private final StubSkuQueryPort stubSkuQueryPort;

    public DatabaseResetHook(CartJpaRepository cartJpaRepository,
                             CartTestContext context,
                             StubSkuQueryPort stubSkuQueryPort) {
        this.cartJpaRepository = cartJpaRepository;
        this.context = context;
        this.stubSkuQueryPort = stubSkuQueryPort;
    }

    @Before
    public void reset() {
        cartJpaRepository.deleteAll();
        context.setCurrentUserId(null);
        context.setLastCartItemId(null);
        context.setLastStatusCode(0);
        context.clearCartItemIds();
        context.setLastCartItemResponse(null);
        stubSkuQueryPort.clear();
    }
}
