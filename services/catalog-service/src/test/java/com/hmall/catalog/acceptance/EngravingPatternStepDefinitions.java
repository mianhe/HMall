package com.hmall.catalog.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class EngravingPatternStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CatalogTestContext context;

    private final Map<String, Long> patternNameToId = new ConcurrentHashMap<>();

    private ResponseEntity<EngravingPatternApiDto> lastCreateResponse;
    private ResponseEntity<EngravingPatternApiDto> lastUpdateResponse;
    private ResponseEntity<EngravingPatternApiDto> lastGetResponse;
    private ResponseEntity<List<EngravingPatternApiDto>> lastListResponse;
    private ResponseEntity<Void> lastDeleteResponse;

    public EngravingPatternStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    @Before
    public void resetResponses() {
        patternNameToId.clear();
        lastCreateResponse = null;
        lastUpdateResponse = null;
        lastGetResponse = null;
        lastListResponse = null;
        lastDeleteResponse = null;
    }

    // ---------- Given ----------

    @Given("已存在镭雕图案 {string} 图片 {string}")
    public void 已存在镭雕图案图片(String name, String imageUrl) {
        已存在镭雕图案图片排序(name, imageUrl, null);
    }

    @Given("已存在镭雕图案 {string} 图片 {string} 排序 {int}")
    public void 已存在镭雕图案图片排序(String name, String imageUrl, Integer sortOrder) {
        EngravingPatternApiDto.Create body = new EngravingPatternApiDto.Create();
        body.name = name;
        body.imageUrl = imageUrl;
        body.sortOrder = sortOrder;
        body.enabled = true;
        ResponseEntity<EngravingPatternApiDto> res = postPattern(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        assertThat(res.getBody()).isNotNull();
        patternNameToId.put(name, res.getBody().id);
    }

    @Given("已存在镭雕图案 {string} 图片 {string} 启用 {word}")
    public void 已存在镭雕图案图片启用(String name, String imageUrl, String enabledStr) {
        EngravingPatternApiDto.Create body = new EngravingPatternApiDto.Create();
        body.name = name;
        body.imageUrl = imageUrl;
        body.enabled = "true".equalsIgnoreCase(enabledStr);
        ResponseEntity<EngravingPatternApiDto> res = postPattern(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        assertThat(res.getBody()).isNotNull();
        patternNameToId.put(name, res.getBody().id);
    }

    // ---------- When ----------

    @When("用户创建镭雕图案 名称为 {string} 图片为 {string}")
    public void 用户创建镭雕图案名称为图片为(String name, String imageUrl) {
        EngravingPatternApiDto.Create body = new EngravingPatternApiDto.Create();
        body.name = name;
        body.imageUrl = imageUrl;
        lastCreateResponse = postPattern(body);
        context.setLastStatusCode(lastCreateResponse.getStatusCode().value());
        if (lastCreateResponse.getStatusCode().is2xxSuccessful() && lastCreateResponse.getBody() != null) {
            patternNameToId.put(name, lastCreateResponse.getBody().id);
        }
    }

    @When("用户创建镭雕图案 名称为 {string} 图片为 {string} 排序 {int} 启用 {word}")
    public void 用户创建镭雕图案名称为图片为排序启用(String name, String imageUrl, int sortOrder, String enabledStr) {
        EngravingPatternApiDto.Create body = new EngravingPatternApiDto.Create();
        body.name = name;
        body.imageUrl = imageUrl;
        body.sortOrder = sortOrder;
        body.enabled = "true".equalsIgnoreCase(enabledStr);
        lastCreateResponse = postPattern(body);
        context.setLastStatusCode(lastCreateResponse.getStatusCode().value());
        if (lastCreateResponse.getStatusCode().is2xxSuccessful() && lastCreateResponse.getBody() != null) {
            patternNameToId.put(name, lastCreateResponse.getBody().id);
        }
    }

    @When("用户修改镭雕图案 {string} 名称为 {string} 图片为 {string}")
    public void 用户修改镭雕图案名称为图片为(String patternName, String newName, String newImageUrl) {
        Long id = resolvePatternId(patternName);
        EngravingPatternApiDto.Update body = new EngravingPatternApiDto.Update();
        body.name = newName;
        body.imageUrl = newImageUrl;
        lastUpdateResponse = putPattern(id, body);
        context.setLastStatusCode(lastUpdateResponse.getStatusCode().value());
    }

    @When("用户删除镭雕图案 {string}")
    public void 用户删除镭雕图案(String patternName) {
        Long id = resolvePatternId(patternName);
        lastDeleteResponse = deletePattern(id);
        context.setLastStatusCode(lastDeleteResponse.getStatusCode().value());
        if (lastDeleteResponse.getStatusCode().is2xxSuccessful()) {
            patternNameToId.remove(patternName);
        }
    }

    @When("用户按 ID 查询镭雕图案 {string}")
    public void 用户按ID查询镭雕图案(String patternName) {
        Long id = patternNameToId.get(patternName);
        if (id == null) {
            lastGetResponse = ResponseEntity.status(404).body(null);
            context.setLastStatusCode(404);
            return;
        }
        try {
            lastGetResponse = restTemplate.getForEntity(baseUrl() + "/api/engraving-patterns/" + id, EngravingPatternApiDto.class);
        } catch (RestClientResponseException e) {
            lastGetResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastGetResponse.getStatusCode().value());
    }

    @When("用户查询镭雕图案列表")
    public void 用户查询镭雕图案列表() {
        try {
            lastListResponse = restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EngravingPatternApiDto>>() {});
        } catch (RestClientResponseException e) {
            lastListResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastListResponse.getStatusCode().value());
    }

    @When("用户查询启用的镭雕图案列表")
    public void 用户查询启用的镭雕图案列表() {
        try {
            lastListResponse = restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns?enabled=true",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EngravingPatternApiDto>>() {});
        } catch (RestClientResponseException e) {
            lastListResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastListResponse.getStatusCode().value());
    }

    @When("用户修改不存在的镭雕图案 名称为 {string} 图片为 {string}")
    public void 用户修改不存在的镭雕图案名称为图片为(String name, String imageUrl) {
        EngravingPatternApiDto.Update body = new EngravingPatternApiDto.Update();
        body.name = name;
        body.imageUrl = imageUrl;
        try {
            lastUpdateResponse = restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns/999999",
                HttpMethod.PUT, new HttpEntity<>(body, jsonHeaders()),
                EngravingPatternApiDto.class);
        } catch (RestClientResponseException e) {
            lastUpdateResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastUpdateResponse != null ? lastUpdateResponse.getStatusCode().value() : 404);
    }

    @When("用户删除不存在的镭雕图案")
    public void 用户删除不存在的镭雕图案() {
        try {
            lastDeleteResponse = restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns/999999",
                HttpMethod.DELETE, null, Void.class);
        } catch (RestClientResponseException e) {
            lastDeleteResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastDeleteResponse != null ? lastDeleteResponse.getStatusCode().value() : 404);
    }

    // ---------- Then ----------

    @Then("镭雕图案应创建成功")
    public void 镭雕图案应创建成功() {
        assertThat(lastCreateResponse).isNotNull();
        assertThat(lastCreateResponse.getStatusCode().value()).isEqualTo(201);
    }

    @And("返回的图案名称为 {string}")
    public void 返回的图案名称为(String expected) {
        EngravingPatternApiDto body = lastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.name).isEqualTo(expected);
    }

    @And("返回的图片地址为 {string}")
    public void 返回的图片地址为(String expected) {
        EngravingPatternApiDto body = lastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.imageUrl).isEqualTo(expected);
    }

    @And("返回的 enabled 为 {word}")
    public void 返回的enabled为(String expected) {
        EngravingPatternApiDto body = lastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.enabled).isEqualTo("true".equalsIgnoreCase(expected));
    }

    @And("返回的 sortOrder 为 {int}")
    public void 返回的sortOrder为(int expected) {
        EngravingPatternApiDto body = lastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.sortOrder).isEqualTo(expected);
    }

    @Then("镭雕图案应修改成功")
    public void 镭雕图案应修改成功() {
        assertThat(lastUpdateResponse).isNotNull();
        assertThat(lastUpdateResponse.getStatusCode().value()).isEqualTo(200);
    }

    @Then("镭雕图案应删除成功")
    public void 镭雕图案应删除成功() {
        assertThat(lastDeleteResponse).isNotNull();
        assertThat(lastDeleteResponse.getStatusCode().value()).isIn(200, 204);
    }

    @Then("应返回 {int} 个图案")
    public void 应返回个图案(int count) {
        assertThat(lastListResponse).isNotNull();
        assertThat(lastListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastListResponse.getBody()).hasSize(count);
    }

    @And("第一个图案名称为 {string}")
    public void 第一个图案名称为(String expected) {
        assertThat(lastListResponse.getBody()).isNotEmpty();
        assertThat(lastListResponse.getBody().get(0).name).isEqualTo(expected);
    }

    @And("第二个图案名称为 {string}")
    public void 第二个图案名称为(String expected) {
        assertThat(lastListResponse.getBody()).size().isGreaterThanOrEqualTo(2);
        assertThat(lastListResponse.getBody().get(1).name).isEqualTo(expected);
    }

    @Then("应返回图案详情")
    public void 应返回图案详情() {
        assertThat(lastGetResponse).isNotNull();
        assertThat(lastGetResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastGetResponse.getBody()).isNotNull();
    }

    // ---------- 辅助 ----------

    private Long resolvePatternId(String patternName) {
        Long id = patternNameToId.get(patternName);
        assertThat(id).as("镭雕图案「%s」应先存在", patternName).isNotNull();
        return id;
    }

    private EngravingPatternApiDto lastResponseBody() {
        if (lastCreateResponse != null && lastCreateResponse.getBody() != null) return lastCreateResponse.getBody();
        if (lastUpdateResponse != null && lastUpdateResponse.getBody() != null) return lastUpdateResponse.getBody();
        if (lastGetResponse != null && lastGetResponse.getBody() != null) return lastGetResponse.getBody();
        return null;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private ResponseEntity<EngravingPatternApiDto> postPattern(EngravingPatternApiDto.Create body) {
        try {
            return restTemplate.postForEntity(baseUrl() + "/api/engraving-patterns", body, EngravingPatternApiDto.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<EngravingPatternApiDto> putPattern(Long id, EngravingPatternApiDto.Update body) {
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns/" + id,
                HttpMethod.PUT, new HttpEntity<>(body, jsonHeaders()),
                EngravingPatternApiDto.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<Void> deletePattern(Long id) {
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/engraving-patterns/" + id,
                HttpMethod.DELETE, null, Void.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static class EngravingPatternApiDto {
        public Long id;
        public String name;
        public String imageUrl;
        public Integer sortOrder;
        public boolean enabled;

        public static class Create {
            public String name;
            public String imageUrl;
            public Integer sortOrder;
            public Boolean enabled;
        }

        public static class Update {
            public String name;
            public String imageUrl;
            public Integer sortOrder;
            public Boolean enabled;
        }
    }
}
