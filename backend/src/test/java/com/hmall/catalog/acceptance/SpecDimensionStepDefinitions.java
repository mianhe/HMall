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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规格维度与选项 feature 的 Step Definitions，按 catalog-api.yaml 调用 API。
 */
public class SpecDimensionStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CatalogTestContext context;
    private final ObjectMapper objectMapper;

    private ResponseEntity<SpecDimensionApiDto.DimensionResponse> lastCreateDimensionResponse;
    private ResponseEntity<SpecDimensionApiDto.OptionResponse> lastCreateOptionResponse;
    private ResponseEntity<List<SpecDimensionApiDto.DimensionWithOptions>> lastDimensionsListResponse;
    private ResponseEntity<SpecDimensionApiDto.Error> lastDimensionErrorResponse;
    private ResponseEntity<SpecDimensionApiDto.Error> lastOptionErrorResponse;

    public SpecDimensionStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context, ObjectMapper objectMapper) {
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

    // ---------- Given: 该 SPU 已有维度（可能含选项） ----------
    @Given("该 SPU 已有维度 {string}")
    public void 该SPU已有维度(String dimensionName) {
        Long spuId = resolveSpuId();
        SpecDimensionApiDto.DimensionCreate body = new SpecDimensionApiDto.DimensionCreate();
        body.name = dimensionName;
        body.required = true;
        body.sortOrder = 1;
        body.affectsAppearance = false;
        ResponseEntity<SpecDimensionApiDto.DimensionResponse> res = postDimension(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            context.putDimensionId(context.getLastProductName(), dimensionName, res.getBody().id);
        }
    }

    @Given("该 SPU 已有维度 {string} 影响外观 是")
    public void 该SPU已有维度影响外观是(String dimensionName) {
        该SPU已有维度影响外观(dimensionName, "是");
    }

    @Given("该 SPU 已有维度 {string} 影响外观 {string}")
    public void 该SPU已有维度影响外观(String dimensionName, String yesNo) {
        Long spuId = resolveSpuId();
        SpecDimensionApiDto.DimensionCreate body = new SpecDimensionApiDto.DimensionCreate();
        body.name = dimensionName;
        body.required = true;
        body.sortOrder = 1;
        body.affectsAppearance = "是".equals(yesNo);
        ResponseEntity<SpecDimensionApiDto.DimensionResponse> res = postDimension(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            context.putDimensionId(context.getLastProductName(), dimensionName, res.getBody().id);
        }
    }

    @Given("该 SPU 已有维度 {string} 含选项 {string}")
    public void 该SPU已有维度含选项一个(String dimensionName, String opt1) {
        该SPU已有维度(dimensionName);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt1, 1, null);
    }

    @Given("该 SPU 已有维度 {string} 含选项 {string} {string}")
    public void 该SPU已有维度含选项两个(String dimensionName, String opt1, String opt2) {
        该SPU已有维度(dimensionName);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt1, 1, null);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt2, 2, null);
    }

    @Given("该 SPU 已有维度 {string} 影响外观 含选项 {string} 图片 {string}")
    public void 该SPU已有维度影响外观含选项图片(String dimensionName, String optionValue, String imageUrl) {
        该SPU已有维度影响外观(dimensionName, "是");
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, optionValue, 1, imageUrl);
    }

    @Given("该 SPU 已有维度 {string} 必填 含选项 {string} {string}")
    public void 该SPU已有维度必填含选项(String dimensionName, String opt1, String opt2) {
        该SPU已有维度(dimensionName);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt1, 1, null);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt2, 2, null);
    }

    @Given("该 SPU 已有维度 {string} 必填 含选项 {string}")
    public void 该SPU已有维度必填含选项一个(String dimensionName, String opt1) {
        该SPU已有维度(dimensionName);
        addOptionForCurrentDimension(context.getLastProductName(), dimensionName, opt1, 1, null);
    }

    @Given("该 SPU 已有维度 {string} {string} 及选项")
    public void 该SPU已有维度及选项(String dim1, String dim2) {
        该SPU已有维度含选项两个(dim1, "128G", "256G");
        该SPU已有维度含选项两个(dim2, "黑色", "白色");
    }

    private void addOptionForCurrentDimension(String productName, String dimensionName, String optionValue, int sortOrder, String imageUrl) {
        Long spuId = context.getSpuId(productName);
        Long dimensionId = context.getDimensionId(productName, dimensionName);
        assertThat(spuId).isNotNull();
        assertThat(dimensionId).isNotNull();
        SpecDimensionApiDto.OptionCreate body = new SpecDimensionApiDto.OptionCreate();
        body.optionValue = optionValue;
        body.sortOrder = sortOrder;
        body.image = imageUrl;
        ResponseEntity<SpecDimensionApiDto.OptionResponse> res = postOption(spuId, dimensionId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            context.putOptionId(productName, dimensionName, optionValue, res.getBody().id);
        }
    }

    // ---------- When: 添加维度 ----------
    @When("用户为该 SPU 添加规格维度 名称 {string} 必填 是 排序 {int}")
    public void 用户为该SPU添加规格维度必填是排序(String name, int sortOrder) {
        用户为该SPU添加规格维度(name, "是", sortOrder);
    }

    @When("用户为该 SPU 添加规格维度 名称 {string} 必填 {string} 排序 {int}")
    public void 用户为该SPU添加规格维度(String name, String requiredStr, int sortOrder) {
        Long spuId = resolveSpuId();
        SpecDimensionApiDto.DimensionCreate body = new SpecDimensionApiDto.DimensionCreate();
        body.name = name;
        body.required = "是".equals(requiredStr);
        body.sortOrder = sortOrder;
        body.affectsAppearance = false;
        lastCreateDimensionResponse = postDimension(spuId, body);
        lastCreateOptionResponse = null;
        context.setLastStatusCode(lastCreateDimensionResponse.getStatusCode().value());
        if (lastCreateDimensionResponse.getStatusCode().is2xxSuccessful() && lastCreateDimensionResponse.getBody() != null) {
            context.putDimensionId(context.getLastProductName(), name, lastCreateDimensionResponse.getBody().id);
        }
    }

    @When("用户为 SPU ID {long} 添加规格维度 名称 {string} 必填 是")
    public void 用户为SPU_ID添加规格维度必填是(long spuId, String name) {
        用户为SPU_ID添加规格维度(spuId, name, "是");
    }

    @When("用户为 SPU ID {long} 添加规格维度 名称 {string} 必填 {string}")
    public void 用户为SPU_ID添加规格维度(long spuId, String name, String requiredStr) {
        SpecDimensionApiDto.DimensionCreate body = new SpecDimensionApiDto.DimensionCreate();
        body.name = name;
        body.required = "是".equals(requiredStr);
        body.sortOrder = 1;
        body.affectsAppearance = false;
        lastCreateDimensionResponse = postDimension(spuId, body);
        lastCreateOptionResponse = null;
        context.setLastStatusCode(lastCreateDimensionResponse.getStatusCode().value());
    }

    @When("用户为该 SPU 再添加规格维度 名称 {string} 必填 否")
    public void 用户为该SPU再添加规格维度必填否(String name) {
        用户为该SPU再添加规格维度(name, "否");
    }

    @When("用户为该 SPU 再添加规格维度 名称 {string} 必填 {string}")
    public void 用户为该SPU再添加规格维度(String name, String requiredStr) {
        Long spuId = resolveSpuId();
        SpecDimensionApiDto.DimensionCreate body = new SpecDimensionApiDto.DimensionCreate();
        body.name = name;
        body.required = "是".equals(requiredStr);
        body.sortOrder = 2;
        lastCreateDimensionResponse = postDimension(spuId, body);
        lastCreateOptionResponse = null;
        context.setLastStatusCode(lastCreateDimensionResponse.getStatusCode().value());
    }

    // ---------- When: 添加选项 ----------
    @When("用户为维度 {string} 添加可选项 {string} 排序 {int}")
    public void 用户为维度添加可选项(String dimensionName, String optionValue, int sortOrder) {
        用户为维度添加可选项图片(dimensionName, optionValue, sortOrder, null);
    }

    @When("用户为维度 {string} 添加可选项 {string} 排序 {int} 图片 {string}")
    public void 用户为维度添加可选项图片(String dimensionName, String optionValue, int sortOrder, String imageUrl) {
        Long spuId = resolveSpuId();
        Long dimensionId = context.getDimensionId(context.getLastProductName(), dimensionName);
        assertThat(dimensionId).isNotNull();
        SpecDimensionApiDto.OptionCreate body = new SpecDimensionApiDto.OptionCreate();
        body.optionValue = optionValue;
        body.sortOrder = sortOrder;
        body.image = imageUrl;
        lastCreateOptionResponse = postOption(spuId, dimensionId, body);
        lastCreateDimensionResponse = null;
        context.setLastStatusCode(lastCreateOptionResponse.getStatusCode().value());
        if (lastCreateOptionResponse.getStatusCode().is2xxSuccessful() && lastCreateOptionResponse.getBody() != null) {
            context.putOptionId(context.getLastProductName(), dimensionName, optionValue, lastCreateOptionResponse.getBody().id);
        }
    }

    @When("用户为维度 {string} 再添加可选项 {string} 排序 {int}")
    public void 用户为维度再添加可选项(String dimensionName, String optionValue, int sortOrder) {
        Long spuId = resolveSpuId();
        Long dimensionId = context.getDimensionId(context.getLastProductName(), dimensionName);
        SpecDimensionApiDto.OptionCreate body = new SpecDimensionApiDto.OptionCreate();
        body.optionValue = optionValue;
        body.sortOrder = sortOrder;
        body.image = null;
        lastCreateOptionResponse = postOption(spuId, dimensionId, body);
        lastCreateDimensionResponse = null;
        context.setLastStatusCode(lastCreateOptionResponse.getStatusCode().value());
    }

    @When("用户为维度 ID {long} 添加可选项 {string} 排序 {int}")
    public void 用户为维度ID添加可选项(long dimensionId, String optionValue, int sortOrder) {
        Long spuId = resolveSpuId();
        SpecDimensionApiDto.OptionCreate body = new SpecDimensionApiDto.OptionCreate();
        body.optionValue = optionValue;
        body.sortOrder = sortOrder;
        body.image = null;
        lastCreateOptionResponse = postOption(spuId, dimensionId, body);
        lastCreateDimensionResponse = null;
        context.setLastStatusCode(lastCreateOptionResponse.getStatusCode().value());
    }

    // ---------- When: 请求维度列表 ----------
    @When("用户请求该 SPU 下的所有维度及选项")
    public void 用户请求该SPU下的所有维度及选项() {
        Long spuId = resolveSpuId();
        lastDimensionsListResponse = getDimensions(spuId);
        if (lastDimensionsListResponse != null) {
            context.setLastStatusCode(lastDimensionsListResponse.getStatusCode().value());
        }
    }

    // ---------- Then: 返回字段（创建成功由 CommonAssertionStepDefinitions 的 应创建成功 断言 lastStatusCode） ----------
    @And("返回的维度名称为 {string}")
    public void 返回的维度名称为(String expectedName) {
        assertThat(lastCreateDimensionResponse.getBody()).isNotNull();
        assertThat(lastCreateDimensionResponse.getBody().name).isEqualTo(expectedName);
    }

    @And("返回的选项值为 {string}")
    public void 返回的选项值为(String expectedValue) {
        assertThat(lastCreateOptionResponse.getBody()).isNotNull();
        assertThat(lastCreateOptionResponse.getBody().optionValue).isEqualTo(expectedValue);
    }

    @And("返回的选项图片为 {string}")
    public void 返回的选项图片为(String expectedImage) {
        assertThat(lastCreateOptionResponse.getBody()).isNotNull();
        assertThat(lastCreateOptionResponse.getBody().image).isEqualTo(expectedImage);
    }

    // ---------- Then: 维度列表断言 ----------
    @Then("应返回 {int} 个维度")
    public void 应返回个维度(int count) {
        assertThat(lastDimensionsListResponse).isNotNull();
        assertThat(lastDimensionsListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastDimensionsListResponse.getBody()).isNotNull();
        assertThat(lastDimensionsListResponse.getBody()).hasSize(count);
    }

    @And("返回的维度列表中包含 {string} 和 {string}")
    public void 返回的维度列表中包含(String name1, String name2) {
        List<SpecDimensionApiDto.DimensionWithOptions> list = lastDimensionsListResponse.getBody();
        assertThat(list).isNotNull();
        assertThat(list.stream().map(d -> d.name).toList()).containsExactlyInAnyOrder(name1, name2);
    }

    @And("维度 {string} 下包含选项 {string} 和 {string}")
    public void 维度下包含选项(String dimensionName, String opt1, String opt2) {
        List<SpecDimensionApiDto.DimensionWithOptions> list = lastDimensionsListResponse.getBody();
        assertThat(list).isNotNull();
        SpecDimensionApiDto.DimensionWithOptions dim = list.stream().filter(d -> dimensionName.equals(d.name)).findFirst().orElse(null);
        assertThat(dim).isNotNull();
        assertThat(dim.options).isNotNull();
        assertThat(dim.options.stream().map(o -> o.optionValue).toList()).containsExactlyInAnyOrder(opt1, opt2);
    }

    @And("维度 {string} 下选项 {string} 的图片为 {string}")
    public void 维度下选项的图片为(String dimensionName, String optionValue, String expectedImage) {
        List<SpecDimensionApiDto.DimensionWithOptions> list = lastDimensionsListResponse.getBody();
        assertThat(list).isNotNull();
        SpecDimensionApiDto.DimensionWithOptions dim = list.stream().filter(d -> dimensionName.equals(d.name)).findFirst().orElse(null);
        assertThat(dim).isNotNull();
        SpecDimensionApiDto.OptionResponse opt = dim.options.stream().filter(o -> optionValue.equals(o.optionValue)).findFirst().orElse(null);
        assertThat(opt).isNotNull();
        assertThat(opt.image).isEqualTo(expectedImage);
    }

    // ---------- Then: 错误提示 ----------
    @And("应返回错误提示（同 SPU 内维度名称唯一）")
    public void 应返回错误提示同SPU内维度名称唯一() {
        assertThat(lastCreateDimensionResponse).isNotNull();
        assertThat(lastCreateDimensionResponse.getStatusCode().value()).isEqualTo(400);
        if (lastDimensionErrorResponse != null && lastDimensionErrorResponse.getBody() != null) {
            assertThat(lastDimensionErrorResponse.getBody().message).isNotBlank();
        }
    }

    @And("应返回错误提示（同维度内选项值唯一）")
    public void 应返回错误提示同维度内选项值唯一() {
        assertThat(lastCreateOptionResponse).isNotNull();
        assertThat(lastCreateOptionResponse.getStatusCode().value()).isEqualTo(400);
        if (lastOptionErrorResponse != null && lastOptionErrorResponse.getBody() != null) {
            assertThat(lastOptionErrorResponse.getBody().message).isNotBlank();
        }
    }

    // ---------- API 调用 ----------
    private ResponseEntity<SpecDimensionApiDto.DimensionResponse> postDimension(Long spuId, SpecDimensionApiDto.DimensionCreate body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + spuId + "/dimensions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                SpecDimensionApiDto.DimensionResponse.class
            );
        } catch (RestClientResponseException e) {
            lastDimensionErrorResponse = ResponseEntity.status(e.getStatusCode()).body(parseErrorBody(e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<SpecDimensionApiDto.OptionResponse> postOption(Long spuId, Long dimensionId, SpecDimensionApiDto.OptionCreate body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + spuId + "/dimensions/" + dimensionId + "/options",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                SpecDimensionApiDto.OptionResponse.class
            );
        } catch (RestClientResponseException e) {
            lastOptionErrorResponse = ResponseEntity.status(e.getStatusCode()).body(parseErrorBody(e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<List<SpecDimensionApiDto.DimensionWithOptions>> getDimensions(Long spuId) {
        ParameterizedTypeReference<List<SpecDimensionApiDto.DimensionWithOptions>> type = new ParameterizedTypeReference<>() {};
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + spuId + "/dimensions",
                HttpMethod.GET,
                null,
                type
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private SpecDimensionApiDto.Error parseErrorBody(String json) {
        if (json == null || json.isBlank()) {
            SpecDimensionApiDto.Error e = new SpecDimensionApiDto.Error();
            e.message = "";
            return e;
        }
        try {
            return objectMapper.readValue(json, SpecDimensionApiDto.Error.class);
        } catch (Exception ignored) {
            SpecDimensionApiDto.Error e = new SpecDimensionApiDto.Error();
            e.message = json;
            return e;
        }
    }

}
