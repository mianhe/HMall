package com.hmall.promotion.acceptance;

import com.hmall.promotion.acceptance.config.PromotionTestContext;
import com.hmall.promotion.infrastructure.persistence.CouponEntity;
import com.hmall.promotion.infrastructure.persistence.CouponJpaRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CouponStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final PromotionTestContext testContext;
    private final CouponJpaRepository couponJpaRepository;

    public CouponStepDefinitions(
            TestRestTemplate restTemplate,
            PromotionTestContext testContext,
            CouponJpaRepository couponJpaRepository) {
        this.restTemplate = restTemplate;
        this.testContext = testContext;
        this.couponJpaRepository = couponJpaRepository;
    }

    @假如("已存在以下活跃券模板:")
    public void setupActiveTemplate(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        Map<String, Object> body = new HashMap<>();
        body.put("name", row.get("name"));
        body.put("type", row.get("type"));
        body.put("thresholdCents", Long.parseLong(row.get("thresholdCents")));
        if (row.containsKey("discountCents") && row.get("discountCents") != null) {
            body.put("discountCents", Long.parseLong(row.get("discountCents")));
        }
        if (row.containsKey("discountRate") && row.get("discountRate") != null) {
            body.put("discountRate", Double.parseDouble(row.get("discountRate")));
        }
        if (row.containsKey("maxDiscountCents") && row.get("maxDiscountCents") != null) {
            body.put("maxDiscountCents", Long.parseLong(row.get("maxDiscountCents")));
        }
        body.put("totalQuantity", Integer.parseInt(row.get("totalQuantity")));
        body.put("perUserLimit", Integer.parseInt(row.get("perUserLimit")));
        body.put("validDays", Integer.parseInt(row.get("validDays")));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates", body, Map.class);
        assertNotNull(response.getBody());
        testContext.setLastTemplateId(((Number) response.getBody().get("id")).longValue());
    }

    @假如("已存在以下已停用券模板:")
    public void setupInactiveTemplate(DataTable table) {
        setupActiveTemplate(table);
        Long templateId = testContext.getLastTemplateId();
        restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + templateId + "/deactivate", null, Map.class);
    }

    @假如("用户 {long} 已领取该模板 {int} 张优惠券")
    public void userAlreadyClaimed(long userId, int quantity) {
        Long templateId = testContext.getLastTemplateId();
        Map<String, Object> body = Map.of("userId", userId, "quantity", quantity);
        restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + templateId + "/issue", body, List.class);
    }

    @假如("管理员向用户 {long} 发放了一张已过期的优惠券")
    public void issueExpiredCoupon(long userId) {
        Long templateId = testContext.getLastTemplateId();
        Map<String, Object> body = Map.of("userId", userId, "quantity", 1);
        ResponseEntity<List> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + templateId + "/issue", body, List.class);
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> couponMap = (Map<String, Object>) response.getBody().getFirst();
        Long couponId = ((Number) couponMap.get("id")).longValue();
        testContext.setLastCouponId(couponId);

        CouponEntity entity = couponJpaRepository.findById(couponId).orElseThrow();
        entity.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        couponJpaRepository.save(entity);
    }

    @当("管理员向用户 {long} 发放 {int} 张该模板的优惠券")
    @SuppressWarnings("unchecked")
    public void adminIssueCoupons(long userId, int quantity) {
        Long templateId = testContext.getLastTemplateId();
        Map<String, Object> body = Map.of("userId", userId, "quantity", quantity);
        ResponseEntity<List> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + templateId + "/issue", body, List.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        if (response.getBody() != null && !response.getBody().isEmpty()) {
            Map<String, Object> firstCoupon = (Map<String, Object>) response.getBody().getFirst();
            testContext.setLastCouponId(((Number) firstCoupon.get("id")).longValue());
        }
        testContext.setLastResponseBody(response.getBody() != null
                ? Map.of("coupons", response.getBody()) : null);
    }

    @当("用户 {long} 领取该模板的优惠券")
    public void userClaimCoupon(long userId) {
        Long templateId = testContext.getLastTemplateId();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + templateId + "/claim?userId=" + userId,
                null, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("查询用户 {long} 的优惠券列表")
    public void queryMyCoupons(long userId) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/promotion/coupons/my?userId=" + userId, List.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody() != null
                ? Map.of("coupons", response.getBody()) : null);
    }

    @当("查询用户 {long} 状态为 {word} 的优惠券")
    public void queryMyCouponsByStatus(long userId, String status) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/promotion/coupons/my?userId=" + userId + "&status=" + status, List.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody() != null
                ? Map.of("coupons", response.getBody()) : null);
    }

    @当("查询用户 {long} 的可领券模板列表")
    public void queryClaimableTemplates(long userId) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/promotion/coupon-templates/claimable?userId=" + userId, List.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody() != null
                ? Map.of("templates", response.getBody()) : null);
    }

    @当("执行优惠券过期扫描")
    public void runExpireScan() {
        restTemplate.postForEntity("/api/promotion/coupons/expire-scan", null, Map.class);
    }

    @那么("返回的优惠券列表长度为 {int}")
    public void assertCouponListSize(int expectedSize) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> coupons = (List<Map>) body.get("coupons");
        assertNotNull(coupons);
        assertEquals(expectedSize, coupons.size());
    }

    @那么("返回的优惠券列表长度至少为 {int}")
    public void assertCouponListSizeAtLeast(int minSize) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> coupons = (List<Map>) body.get("coupons");
        assertNotNull(coupons);
        assertTrue(coupons.size() >= minSize,
                "Expected at least " + minSize + " coupons, got " + coupons.size());
    }

    @并且("返回的第一张优惠券状态为 {string}")
    public void assertFirstCouponStatus(String expectedStatus) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> coupons = (List<Map>) body.get("coupons");
        assertEquals(expectedStatus, coupons.getFirst().get("status"));
    }

    @并且("返回的第一张优惠券用户ID为 {long}")
    public void assertFirstCouponUserId(long expectedUserId) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> coupons = (List<Map>) body.get("coupons");
        assertEquals(expectedUserId, ((Number) coupons.getFirst().get("userId")).longValue());
    }

    @并且("返回的优惠券状态为 {string}")
    public void assertCouponStatus(String expectedStatus) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedStatus, body.get("status"));
    }

    @并且("返回的可领券模板数量至少为 {int}")
    public void assertClaimableTemplateCountAtLeast(int minCount) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> templates = (List<Map>) body.get("templates");
        assertNotNull(templates);
        assertTrue(templates.size() >= minCount,
                "Expected at least " + minCount + " claimable templates, got " + templates.size());
    }

    @那么("用户 {long} 的该优惠券状态变为 {string}")
    public void assertCouponExpired(long userId, String expectedStatus) {
        Long couponId = testContext.getLastCouponId();
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/promotion/coupons/my?userId=" + userId + "&status=" + expectedStatus, List.class);
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<Map> coupons = (List<Map>) response.getBody();
        boolean found = coupons.stream()
                .anyMatch(c -> ((Number) c.get("id")).longValue() == couponId);
        assertTrue(found, "Expected coupon " + couponId + " to have status " + expectedStatus);
    }
}
