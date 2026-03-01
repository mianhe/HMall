package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import io.cucumber.java.zh_cn.并且;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共步骤：用户登录状态、SKU 存在性、状态码断言。
 */
public class CommonStepDefinitions {

    private final CartTestContext context;
    private final StubSkuQueryPort stubSkuQueryPort;

    public CommonStepDefinitions(CartTestContext context, StubSkuQueryPort stubSkuQueryPort) {
        this.context = context;
        this.stubSkuQueryPort = stubSkuQueryPort;
    }

    @假如("用户已登录，userId 为 {long}")
    public void 用户已登录(long userId) {
        context.setCurrentUserId(userId);
    }

    @假如("用户未登录")
    public void 用户未登录() {
        context.setCurrentUserId(null);
    }

    @假如("Catalog 中存在 SKU {long}")
    public void catalog中存在SKU(long skuId) {
        stubSkuQueryPort.register(skuId);
    }

    @假如("Catalog 中存在 SKU {long}，名称 {string}，价格 {int}")
    public void catalog中存在SKU带信息(long skuId, String name, int price) {
        stubSkuQueryPort.register(skuId, name, BigDecimal.valueOf(price));
    }

    @假如("Catalog 中存在实体 SKU {long}，spuId {long}，名称 {string}，价格 {int}")
    public void catalog中存在实体SKU(long skuId, long spuId, String name, int price) {
        stubSkuQueryPort.register(skuId, spuId, "PHYSICAL", name, BigDecimal.valueOf(price));
    }

    @假如("Catalog 中存在服务 SKU {long}，spuId {long}，名称 {string}，价格 {int}")
    public void catalog中存在服务SKU(long skuId, long spuId, String name, int price) {
        stubSkuQueryPort.register(skuId, spuId, "SERVICE", name, BigDecimal.valueOf(price));
    }

    @并且("实体 spuId {long} 的可选服务包含 服务spuId {long} 名称 {string} 服务skuId {long} 价格 {int}")
    public void 实体spu可选服务包含(long targetSpuId, long serviceSpuId, String serviceName, long serviceSkuId, int price) {
        stubSkuQueryPort.registerAvailableServices(
            targetSpuId,
            List.of(new com.hmall.cart.application.port.SkuQueryPort.AvailableService(
                serviceSpuId,
                serviceName,
                List.of(new com.hmall.cart.application.port.SkuQueryPort.AvailableServiceSku(
                    1L, serviceSkuId, BigDecimal.valueOf(price)
                ))
            ))
        );
    }

    @假如("Catalog 中不存在 SKU {long}")
    public void catalog中不存在SKU(long skuId) {
        stubSkuQueryPort.unregister(skuId);
    }

    @假如("SKU {long} 已下架")
    public void sku已下架(long skuId) {
        stubSkuQueryPort.markUnavailable(skuId);
    }

    @那么("应返回 {int}")
    public void 应返回状态码(int expectedStatus) {
        assertThat(context.getLastStatusCode()).isEqualTo(expectedStatus);
    }
}
