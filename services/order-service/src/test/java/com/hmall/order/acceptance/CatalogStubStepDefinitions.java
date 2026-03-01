package com.hmall.order.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.en.Given;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用 Catalog 桩：WireMock 模拟 Catalog GET /api/skus/{id}。
 */
public class CatalogStubStepDefinitions {

    private final WireMockServer wireMock;
    private final Map<String, Long> productNameToSkuId = new ConcurrentHashMap<>();

    public CatalogStubStepDefinitions(WireMockServer wireMock) {
        this.wireMock = wireMock;
    }

    @Given("Catalog 已有 SKU {long} 价格 {long} 分 名称 {string}")
    public void catalog已有Sku(long skuId, long priceCents, String displayName) {
        String json = """
            {"id":%d,"spuId":1,"priceCents":%d,"displayName":"%s","spuName":"商品","specValues":[],"productType":"PHYSICAL"}
            """.formatted(skuId, priceCents, displayName.replace("\"", "\\\""));
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/skus/" + skuId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json))
        );
    }

    @Given("Catalog 已有商品 {string} skuId {long} 价格 {long} 分")
    public void catalog已有商品(String productName, long skuId, long priceCents) {
        productNameToSkuId.put(productName, skuId);
        String escaped = productName.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = """
            {"id":%d,"spuId":1,"priceCents":%d,"displayName":"%s","spuName":"%s","specValues":[],"productType":"PHYSICAL"}
            """.formatted(skuId, priceCents, escaped, escaped);
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/skus/" + skuId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json))
        );
    }

    @Given("Catalog 已有服务商品 {string} skuId {long} 价格 {long} 分")
    public void catalog已有服务商品(String productName, long skuId, long priceCents) {
        productNameToSkuId.put(productName, skuId);
        String escaped = productName.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = """
            {"id":%d,"spuId":1,"priceCents":%d,"displayName":"%s","spuName":"%s","specValues":[],"productType":"SERVICE"}
            """.formatted(skuId, priceCents, escaped, escaped);
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/skus/" + skuId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json))
        );
    }

    @Given("Catalog 中 skuId {long} 不存在")
    public void catalog中SkuId不存在(long skuId) {
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/skus/" + skuId))
                .willReturn(WireMock.aResponse().withStatus(404))
        );
    }

    @Given("Catalog 中实体 spuId {long} 绑定服务 skuId {long} 价格 {long} 分")
    public void catalog中实体SpuId绑定服务SkuId价格(long targetSpuId, long serviceSkuId, long priceCents) {
        String json = """
            [{"serviceSpuId":21,"name":"延保服务","bindings":[{"bindingId":1,"serviceSkuId":%d,"priceCents":%d}]}]
            """.formatted(serviceSkuId, priceCents);
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/products/" + targetSpuId + "/available-services"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json))
        );
    }

    public Long getSkuId(String productName) {
        return productNameToSkuId.get(productName);
    }
}
