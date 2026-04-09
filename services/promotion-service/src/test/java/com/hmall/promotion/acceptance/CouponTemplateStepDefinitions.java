package com.hmall.promotion.acceptance;

import com.hmall.promotion.acceptance.config.PromotionTestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CouponTemplateStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final PromotionTestContext testContext;

    public CouponTemplateStepDefinitions(TestRestTemplate restTemplate, PromotionTestContext testContext) {
        this.restTemplate = restTemplate;
        this.testContext = testContext;
    }

    @当("管理员创建满减券模板:")
    public void createAmountOffTemplate(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        Map<String, Object> body = new HashMap<>();
        body.put("name", row.get("name"));
        body.put("type", "AMOUNT_OFF");
        body.put("thresholdCents", Long.parseLong(row.get("thresholdCents")));
        body.put("discountCents", Long.parseLong(row.get("discountCents")));
        body.put("totalQuantity", Integer.parseInt(row.get("totalQuantity")));
        body.put("perUserLimit", Integer.parseInt(row.get("perUserLimit")));
        body.put("validDays", Integer.parseInt(row.get("validDays")));
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/promotion/coupon-templates", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("管理员创建折扣券模板:")
    public void createPercentageOffTemplate(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        Map<String, Object> body = new HashMap<>();
        body.put("name", row.get("name"));
        body.put("type", "PERCENTAGE_OFF");
        body.put("thresholdCents", Long.parseLong(row.get("thresholdCents")));
        body.put("discountRate", Double.parseDouble(row.get("discountRate")));
        if (row.get("maxDiscountCents") != null) {
            body.put("maxDiscountCents", Long.parseLong(row.get("maxDiscountCents")));
        }
        body.put("totalQuantity", Integer.parseInt(row.get("totalQuantity")));
        body.put("perUserLimit", Integer.parseInt(row.get("perUserLimit")));
        body.put("validDays", Integer.parseInt(row.get("validDays")));
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/promotion/coupon-templates", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("管理员创建券模板但缺少名称")
    public void createTemplateWithoutName() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "AMOUNT_OFF");
        body.put("thresholdCents", 10000L);
        body.put("discountCents", 2000L);
        body.put("totalQuantity", 100);
        body.put("perUserLimit", 1);
        body.put("validDays", 30);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/promotion/coupon-templates", body, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @假如("已存在以下券模板:")
    public void setupTemplates(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        Long lastId = null;
        for (Map<String, String> row : rows) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", row.get("name"));
            body.put("type", row.get("type"));
            body.put("thresholdCents", Long.parseLong(row.get("thresholdCents")));
            if (row.containsKey("discountCents") && row.get("discountCents") != null) {
                body.put("discountCents", Long.parseLong(row.get("discountCents")));
            }
            body.put("totalQuantity", Integer.parseInt(row.get("totalQuantity")));
            body.put("perUserLimit", Integer.parseInt(row.get("perUserLimit")));
            body.put("validDays", Integer.parseInt(row.get("validDays")));
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/promotion/coupon-templates", body, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("id")) {
                lastId = ((Number) response.getBody().get("id")).longValue();
            }
        }
        testContext.setLastTemplateId(lastId);
    }

    @当("查询券模板列表")
    public void listTemplates() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/promotion/coupon-templates", Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("查询该券模板详情")
    public void getTemplateDetail() {
        Long id = testContext.getLastTemplateId();
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/promotion/coupon-templates/" + id, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("停用该券模板")
    public void deactivateTemplate() {
        Long id = testContext.getLastTemplateId();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + id + "/deactivate", null, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @当("停用ID为 {int} 的券模板")
    public void deactivateNonExistentTemplate(int id) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/promotion/coupon-templates/" + id + "/deactivate", null, Map.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
        testContext.setLastResponseBody(response.getBody());
    }

    @那么("返回状态码 {int}")
    public void assertStatusCode(int expectedCode) {
        assertEquals(expectedCode, testContext.getLastStatusCode());
    }

    @并且("返回的券模板类型为 {string}")
    public void assertTemplateType(String expectedType) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedType, body.get("type"));
    }

    @并且("返回的券模板状态为 {string}")
    public void assertTemplateStatus(String expectedStatus) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedStatus, body.get("status"));
    }

    @并且("返回的券模板已发放数量为 {int}")
    public void assertIssuedQuantity(int expected) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expected, ((Number) body.get("issuedQuantity")).intValue());
    }

    @并且("返回的券模板数量至少为 {int}")
    public void assertTemplateCountAtLeast(int minCount) {
        Map body = testContext.getLastResponseBody();
        @SuppressWarnings("unchecked")
        List<Map> content = (List<Map>) body.get("content");
        assertNotNull(content);
        assertTrue(content.size() >= minCount,
                "Expected at least " + minCount + " templates, got " + content.size());
    }

    @并且("返回的券模板名称为 {string}")
    public void assertTemplateName(String expectedName) {
        Map body = testContext.getLastResponseBody();
        assertEquals(expectedName, body.get("name"));
    }
}
