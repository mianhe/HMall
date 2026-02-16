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
            {"id":%d,"spuId":1,"priceCents":%d,"displayName":"%s","specValues":[]}
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
        String json = """
            {"id":%d,"spuId":1,"priceCents":%d,"displayName":"%s","specValues":[]}
            """.formatted(skuId, priceCents, productName.replace("\"", "\\\""));
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

    public Long getSkuId(String productName) {
        return productNameToSkuId.get(productName);
    }
}
