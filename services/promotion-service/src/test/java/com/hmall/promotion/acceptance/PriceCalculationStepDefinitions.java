package com.hmall.promotion.acceptance;

import com.hmall.promotion.acceptance.config.PromotionTestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceCalculationStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final PromotionTestContext testContext;

    public PriceCalculationStepDefinitions(TestRestTemplate restTemplate, PromotionTestContext testContext) {
        this.restTemplate = restTemplate;
        this.testContext = testContext;
    }

    @当("计算价格:")
    public void calculatePriceWithCoupon(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        long userId = Long.parseLong(rows.getFirst().get("userId"));
        String couponIdStr = rows.getFirst().get("couponId");
        Long couponId = "latest".equals(couponIdStr) ? testContext.getLastCouponId() : Long.parseLong(couponIdStr);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, String> row : rows) {
            items.add(Map.of(
                    "skuId", Long.parseLong(row.get("skuId")),
                    "unitPriceCents", Long.parseLong(row.get("unitPriceCents")),
                    "quantity", Integer.parseInt(row.get("quantity"))));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("couponId", couponId);
        body.put("items", items);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/calculate-price", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("不传券计算价格:")
    public void calculatePriceWithoutCoupon(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        long userId = Long.parseLong(rows.getFirst().get("userId"));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, String> row : rows) {
            items.add(Map.of(
                    "skuId", Long.parseLong(row.get("skuId")),
                    "unitPriceCents", Long.parseLong(row.get("unitPriceCents")),
                    "quantity", Integer.parseInt(row.get("quantity"))));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("items", items);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/calculate-price", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("创建定向活动:")
    public void createTargetingActivity(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        Map<String, Object> targetingRule = new HashMap<>();
        if (row.get("levelsIn") != null && !row.get("levelsIn").isBlank()) {
            targetingRule.put("levelsIn", Set.of(row.get("levelsIn").split(",")));
        }
        if (row.get("tagsAny") != null && !row.get("tagsAny").isBlank()) {
            targetingRule.put("tagsAny", Set.of(row.get("tagsAny").split(",")));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("name", row.get("name"));
        body.put("type", "SKU_AMOUNT_OFF");
        body.put("targetSkuIds", List.of(Long.parseLong(row.get("targetSkuId"))));
        body.put("discountCents", Long.parseLong(row.get("discountCents")));
        body.put("priority", 0);
        body.put("startAt", "2026-01-01T00:00:00Z");
        body.put("endAt", "2099-01-01T00:00:00Z");
        body.put("targetingRule", targetingRule);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/activities", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
        if (response.getBody() != null && response.getBody().get("id") != null) {
            Long id = ((Number) response.getBody().get("id")).longValue();
            restTemplate.postForEntity("/api/promotion/activities/" + id + "/activate", null, Map.class);
        }
    }

    @当("创建满件活动:")
    public void createPieceActivity(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        Map<String, Object> pieceRule = new HashMap<>();
        pieceRule.put("scopeType", row.get("scopeType"));
        if (row.get("scopeIds") != null && !row.get("scopeIds").isBlank()) {
            pieceRule.put("scopeIds", java.util.Arrays.stream(row.get("scopeIds").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Long::parseLong)
                    .toList());
        }
        pieceRule.put("minQuantity", Integer.parseInt(row.get("minQuantity")));
        pieceRule.put("discountType", row.get("discountType"));
        pieceRule.put("discountValue", Long.parseLong(row.get("discountValue")));
        Map<String, Object> body = new HashMap<>();
        body.put("name", row.get("name"));
        body.put("type", "SKU_AMOUNT_OFF");
        body.put("targetSkuIds", List.of(Long.parseLong(row.get("targetSkuId"))));
        body.put("discountCents", 1L);
        body.put("priority", 0);
        body.put("startAt", "2026-01-01T00:00:00Z");
        body.put("endAt", "2099-01-01T00:00:00Z");
        body.put("pieceRule", pieceRule);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/activities", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
        if (response.getBody() != null && response.getBody().get("id") != null) {
            Long id = ((Number) response.getBody().get("id")).longValue();
            restTemplate.postForEntity("/api/promotion/activities/" + id + "/activate", null, Map.class);
        }
    }

    @当("查询用户 {long} 订单金额 {long} 的可用优惠券")
    public void queryAvailableCoupons(long userId, long orderAmountCents) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/promotion/coupons/available?userId=" + userId + "&orderAmountCents=" + orderAmountCents,
                List.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody() != null
                ? Map.of("coupons", response.getBody()) : null);
    }

    @当("锁定该优惠券关联订单 {long}")
    public void lockCoupon(long orderId) {
        Long couponId = testContext.getLastCouponId();
        Map<String, Object> body = Map.of("orderId", orderId);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupons/" + couponId + "/lock", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @并且("核销该优惠券")
    public void redeemCoupon() {
        Long couponId = testContext.getLastCouponId();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupons/" + couponId + "/redeem", null, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @并且("释放该优惠券")
    public void releaseCoupon() {
        Long couponId = testContext.getLastCouponId();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupons/" + couponId + "/release", null, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @并且("原价为 {long}")
    public void assertOriginalAmount(long expectedAmount) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedAmount, ((Number) body.get("originalAmountCents")).longValue());
    }

    @并且("优惠金额为 {long}")
    public void assertDiscountAmount(long expectedDiscount) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedDiscount, ((Number) body.get("discountAmountCents")).longValue());
    }

    @并且("实付金额为 {long}")
    public void assertPayableAmount(long expectedPayable) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedPayable, ((Number) body.get("payableAmountCents")).longValue());
    }
}
