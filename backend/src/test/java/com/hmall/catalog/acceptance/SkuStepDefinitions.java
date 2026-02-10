package com.hmall.catalog.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SKU feature 的 Step Definitions，按 catalog-api.yaml 调用 API。
 */
public class SkuStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CatalogTestContext context;
    private final ObjectMapper objectMapper;

    private ResponseEntity<SkuApiDto.Response> lastCreateSkuResponse;
    private ResponseEntity<List<SkuApiDto.Response>> lastSkuListResponse;
    private ResponseEntity<SkuApiDto.Response> lastSkuDetailResponse;
    private ResponseEntity<SkuApiDto.Error> lastSkuErrorResponse;

    public SkuStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.context = context != null ? context : new CatalogTestContext();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    private Long resolveSpuId() {
        String productName = context.getLastProductName();
        assertThat(productName).isNotNull();
        Long spuId = context.getSpuId(productName);
        assertThat(spuId).isNotNull();
        return spuId;
    }

    /** 从 context 按「容量 128G」「颜色 黑色」解析出 optionId 列表 */
    private List<Long> resolveSpecOptionIds(String productName, String dim1, String val1, String dim2, String val2) {
        List<Long> ids = new ArrayList<>();
        if (dim1 != null && val1 != null) {
            Long id = context.getOptionId(productName, dim1, val1);
            assertThat(id).isNotNull();
            ids.add(id);
        }
        if (dim2 != null && val2 != null) {
            Long id = context.getOptionId(productName, dim2, val2);
            assertThat(id).isNotNull();
            ids.add(id);
        }
        return ids;
    }

    // ---------- Given: 该 SPU 下已有 SKU ----------
    @Given("该 SPU 下已有 SKU 选择 容量 {string} 颜色 {string} 价格 {long} 分")
    public void 该SPU下已有SKU选择(String cap, String color, long priceCents) {
        String productName = context.getLastProductName();
        Long spuId = resolveSpuId();
        List<Long> optionIds = resolveSpecOptionIds(productName, "容量", cap, "颜色", color);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = optionIds;
        body.priceCents = priceCents;
        body.displayName = null;
        ResponseEntity<SkuApiDto.Response> res = postSku(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            context.putLastSkuId(productName, res.getBody().id);
        }
    }

    // ---------- When: 创建 SKU ----------
    @When("用户在该 SPU 下创建 SKU 选择 容量 {string} 颜色 {string} 价格 {long} 分")
    public void 用户在该SPU下创建SKU选择容量颜色(String cap, String color, long priceCents) {
        String productName = context.getLastProductName();
        Long spuId = resolveSpuId();
        List<Long> optionIds = resolveSpecOptionIds(productName, "容量", cap, "颜色", color);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = optionIds;
        body.priceCents = priceCents;
        body.displayName = null;
        lastCreateSkuResponse = postSku(spuId, body);
        context.setLastStatusCode(lastCreateSkuResponse.getStatusCode().value());
        if (lastCreateSkuResponse.getStatusCode().is2xxSuccessful() && lastCreateSkuResponse.getBody() != null) {
            context.putLastSkuId(productName, lastCreateSkuResponse.getBody().id);
        }
    }

    @When("用户在该 SPU 下创建 SKU 选择 容量 {string} 价格 {long} 分")
    public void 用户在该SPU下创建SKU选择容量(String cap, long priceCents) {
        String productName = context.getLastProductName();
        Long spuId = resolveSpuId();
        List<Long> optionIds = resolveSpecOptionIds(productName, "容量", cap, null, null);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = optionIds;
        body.priceCents = priceCents;
        body.displayName = null;
        lastCreateSkuResponse = postSku(spuId, body);
        context.setLastStatusCode(lastCreateSkuResponse.getStatusCode().value());
        if (lastCreateSkuResponse.getStatusCode().is2xxSuccessful() && lastCreateSkuResponse.getBody() != null) {
            context.putLastSkuId(productName, lastCreateSkuResponse.getBody().id);
        }
    }

    @When("用户在 SPU ID {long} 下创建 SKU 规格 {string} 价格 {long} 分")
    public void 用户在SPU_ID下创建SKU规格(long spuId, String displayName, long priceCents) {
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = List.of();
        body.priceCents = priceCents;
        body.displayName = displayName;
        lastCreateSkuResponse = postSku(spuId, body);
        context.setLastStatusCode(lastCreateSkuResponse.getStatusCode().value());
    }

    @When("用户在该 SPU 下创建 SKU 使用非本 SPU 的 Option 价格 {long} 分")
    public void 用户在该SPU下创建SKU使用非本SPU的Option(long priceCents) {
        Long spuId = resolveSpuId();
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = List.of(999998L, 999999L);
        body.priceCents = priceCents;
        body.displayName = null;
        lastCreateSkuResponse = postSku(spuId, body);
        context.setLastStatusCode(lastCreateSkuResponse.getStatusCode().value());
    }

    // ---------- When: 请求 SKU 列表与详情 ----------
    @When("用户请求该 SPU 下的所有 SKU")
    public void 用户请求该SPU下的所有SKU() {
        Long spuId = resolveSpuId();
        lastSkuListResponse = getSkus(spuId);
        if (lastSkuListResponse != null) {
            context.setLastStatusCode(lastSkuListResponse.getStatusCode().value());
        }
    }

    @When("用户请求该 SKU 的详情")
    public void 用户请求该SKU的详情() {
        String productName = context.getLastProductName();
        Long spuId = resolveSpuId();
        Long skuId = context.getLastSkuId(productName);
        assertThat(skuId).isNotNull();
        lastSkuDetailResponse = getSkuDetail(spuId, skuId);
        context.setLastStatusCode(lastSkuDetailResponse.getStatusCode().value());
    }

    @When("用户请求该 SPU 下 SKU ID {long} 的详情")
    public void 用户请求该SPU下SKU_ID的详情(long skuId) {
        Long spuId = resolveSpuId();
        lastSkuDetailResponse = getSkuDetail(spuId, skuId);
        context.setLastStatusCode(lastSkuDetailResponse.getStatusCode().value());
    }

    // ---------- Then: 创建成功与返回字段 ----------
    @Then("SKU 应创建成功")
    public void SKU应创建成功() {
        assertThat(lastCreateSkuResponse).isNotNull();
        assertThat(lastCreateSkuResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(lastCreateSkuResponse.getBody()).isNotNull();
    }

    @And("返回的 SKU 价格为 {long} 分")
    public void 返回的SKU价格为(long expectedPrice) {
        SkuApiDto.Response body = (lastCreateSkuResponse != null && lastCreateSkuResponse.getBody() != null)
            ? lastCreateSkuResponse.getBody() : lastSkuDetailResponse.getBody();
        assertThat(body).isNotNull();
        assertSkuPrice(expectedPrice, body);
    }

    @And("返回的 SKU 包含所选规格（容量 {string}、颜色 {string}）")
    public void 返回的SKU包含所选规格(String cap, String color) {
        assertSpecValuesContain(lastCreateSkuResponse.getBody(), cap, color);
    }

    @Then("应返回 {int} 个 SKU")
    public void 应返回个SKU(int count) {
        assertThat(lastSkuListResponse).isNotNull();
        assertThat(lastSkuListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastSkuListResponse.getBody()).isNotNull();
        assertThat(lastSkuListResponse.getBody()).hasSize(count);
    }

    @And("返回的 SKU 列表中包含价格 {long} 和 {long} 的 SKU")
    public void 返回的SKU列表中包含价格(long p1, long p2) {
        List<SkuApiDto.Response> list = lastSkuListResponse.getBody();
        assertThat(list).isNotNull();
        assertThat(list.stream().map(s -> s.priceCents).toList()).containsExactlyInAnyOrder(p1, p2);
    }

    @And("应返回 SKU 价格为 {long} 分")
    public void 应返回SKU价格为(long expectedPrice) {
        assertThat(lastSkuDetailResponse.getBody()).isNotNull();
        assertSkuPrice(expectedPrice, lastSkuDetailResponse.getBody());
    }

    @And("应返回 SKU 包含规格 容量 {string} 颜色 {string}")
    public void 应返回SKU包含规格(String cap, String color) {
        assertSpecValuesContain(lastSkuDetailResponse.getBody(), cap, color);
    }

    @And("应返回错误提示（未选齐必填维度：缺少 颜色）")
    public void 应返回错误提示未选齐必填维度缺少颜色() {
        assertCreateSkuBadRequest();
    }

    @And("应返回错误提示（未选齐必填维度：缺少 {string}）")
    public void 应返回错误提示未选齐必填维度(String dimensionName) {
        assertCreateSkuBadRequest();
    }

    @And("应返回错误提示（Option 不属于该 SPU）")
    public void 应返回错误提示Option不属于该SPU() {
        assertCreateSkuBadRequest();
    }

    @And("应返回错误提示（价格不能为负）")
    public void 应返回错误提示价格不能为负() {
        assertCreateSkuBadRequest();
    }

    private void assertSkuPrice(long expectedPrice, SkuApiDto.Response body) {
        assertThat(body.priceCents).isEqualTo(expectedPrice);
    }

    private void assertSpecValuesContain(SkuApiDto.Response body, String cap, String color) {
        assertThat(body).isNotNull();
        assertThat(body.specValues).isNotNull();
        assertThat(body.specValues).anyMatch(s -> "容量".equals(s.dimensionName) && cap.equals(s.optionValue));
        assertThat(body.specValues).anyMatch(s -> "颜色".equals(s.dimensionName) && color.equals(s.optionValue));
    }

    private void assertCreateSkuBadRequest() {
        assertThat(lastCreateSkuResponse.getStatusCode().value()).isEqualTo(400);
        if (lastSkuErrorResponse != null && lastSkuErrorResponse.getBody() != null) {
            assertThat(lastSkuErrorResponse.getBody().message).isNotBlank();
        }
    }

    // ---------- API 与解析 ----------
    private ResponseEntity<SkuApiDto.Response> postSku(Long spuId, SkuApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + spuId + "/skus",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                SkuApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            lastSkuErrorResponse = ResponseEntity.status(e.getStatusCode()).body(parseSkuErrorBody(e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<List<SkuApiDto.Response>> getSkus(Long spuId) {
        ParameterizedTypeReference<List<SkuApiDto.Response>> type = new ParameterizedTypeReference<>() {};
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + spuId + "/skus",
                HttpMethod.GET,
                null,
                type
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<SkuApiDto.Response> getSkuDetail(Long spuId, Long skuId) {
        try {
            return restTemplate.getForEntity(
                baseUrl() + "/api/products/" + spuId + "/skus/" + skuId,
                SkuApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private SkuApiDto.Error parseSkuErrorBody(String json) {
        if (json == null || json.isBlank()) {
            SkuApiDto.Error e = new SkuApiDto.Error();
            e.message = "";
            return e;
        }
        try {
            return objectMapper.readValue(json, SkuApiDto.Error.class);
        } catch (Exception ignored) {
            SkuApiDto.Error e = new SkuApiDto.Error();
            e.message = json;
            return e;
        }
    }

}
