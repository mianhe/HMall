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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理类别 feature 的 Step Definitions，按 catalog-api.yaml 调用 API。
 */
public class CategoryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CatalogTestContext context;

    /** 类别名称 -> 创建后得到的 id（用于“在某某下创建子类别”等步骤） */
    private final Map<String, Long> categoryNameToId = new ConcurrentHashMap<>();

    private ResponseEntity<CategoryApiDto.Response> lastCreateResponse;
    private ResponseEntity<List<CategoryApiDto.Response>> lastListResponse;

    public CategoryStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context != null ? context : new CatalogTestContext();
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    // ---------- 创建根级类别 ----------
    @When("用户创建根级类别 {string}")
    public void 用户创建根级类别(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        lastCreateResponse = postCategory(body);
        if (context != null) {
            context.setLastStatusCode(lastCreateResponse.getStatusCode().value());
        }
        if (lastCreateResponse.getStatusCode().is2xxSuccessful() && lastCreateResponse.getBody() != null) {
            categoryNameToId.put(name, lastCreateResponse.getBody().id);
        }
    }

    @And("返回的类别名称为 {string}")
    public void 返回的类别名称为(String expectedName) {
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().name).isEqualTo(expectedName);
    }

    @And("该类别无父类别")
    public void 该类别无父类别() {
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().parentId).isNull();
    }

    // ---------- 已存在根级类别（Given） ----------
    @Given("已存在根级类别 {string}")
    public void 已存在根级类别(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        ResponseEntity<CategoryApiDto.Response> res = postCategory(body);
        if (context != null) {
            context.setLastStatusCode(res.getStatusCode().value());
        }
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            categoryNameToId.put(name, res.getBody().id);
        }
    }

    // ---------- 在已有类别下创建子类别 ----------
    @When("用户在父类目 ID {long} 下创建子类别 {string}")
    public void 用户在父类目ID下创建子类别(long parentId, String childName) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = childName;
        body.parentId = parentId;
        lastCreateResponse = postCategory(body);
        if (context != null) {
            context.setLastStatusCode(lastCreateResponse.getStatusCode().value());
        }
        if (lastCreateResponse.getStatusCode().is2xxSuccessful() && lastCreateResponse.getBody() != null) {
            categoryNameToId.put(childName, lastCreateResponse.getBody().id);
        }
    }

    @When("用户在类别 {string} 下创建子类别 {string}")
    public void 用户在类别下创建子类别(String parentName, String childName) {
        Long parentId = categoryNameToId.get(parentName);
        assertThat(parentId).as("类别「%s」应先存在", parentName).isNotNull();
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = childName;
        body.parentId = parentId;
        lastCreateResponse = postCategory(body);
        if (context != null) {
            context.setLastStatusCode(lastCreateResponse.getStatusCode().value());
        }
        if (lastCreateResponse.getStatusCode().is2xxSuccessful() && lastCreateResponse.getBody() != null) {
            categoryNameToId.put(childName, lastCreateResponse.getBody().id);
        }
    }

    @And("该类别的父类别为 {string}")
    public void 该类别的父类别为(String parentName) {
        Long expectedParentId = categoryNameToId.get(parentName);
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().parentId).isEqualTo(expectedParentId);
    }

    // ---------- 已存在根级类别「手机」和「平板」 ----------
    @Given("已存在根级类别 {string} 和 {string}")
    public void 已存在根级类别和(String name1, String name2) {
        已存在根级类别(name1);
        已存在根级类别(name2);
    }

    // ---------- 查询根目录下所有类别 ----------
    @When("用户请求根目录下所有类别")
    public void 用户请求根目录下所有类别() {
        lastListResponse = getCategories(null);
    }

    @Then("应返回 {int} 个类别")
    public void 应返回个类别(int count) {
        assertThat(lastListResponse).isNotNull();
        assertThat(lastListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastListResponse.getBody()).hasSize(count);
    }

    @And("返回列表中包含名称 {string} 和 {string}")
    public void 返回列表中包含名称和(String name1, String name2) {
        List<String> names = lastListResponse.getBody().stream()
            .map(c -> c.name)
            .toList();
        assertThat(names).containsExactlyInAnyOrder(name1, name2);
    }

    // ---------- 已存在类别「手机」，且其下有子类别 ----------
    @Given("已存在类别 {string}，且 {string} 下有子类别 {string} 和 {string}")
    public void 已存在类别且下有子类别(String parentName, String _parentAgain, String child1, String child2) {
        已存在根级类别(parentName);
        用户在类别下创建子类别(parentName, child1);
        用户在类别下创建子类别(parentName, child2);
    }

    // ---------- 查询某类别下所有子类别 ----------
    @When("用户请求类别 {string} 下的所有子类别")
    public void 用户请求类别下的所有子类别(String parentName) {
        Long parentId = categoryNameToId.get(parentName);
        assertThat(parentId).as("类别「%s」应先存在", parentName).isNotNull();
        lastListResponse = getCategories(parentId);
    }

    // ---------- API 调用封装 ----------
    private ResponseEntity<CategoryApiDto.Response> postCategory(CategoryApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                CategoryApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private static final ParameterizedTypeReference<List<CategoryApiDto.Response>> LIST_OF_CATEGORY =
        new ParameterizedTypeReference<>() {};

    private ResponseEntity<List<CategoryApiDto.Response>> getCategories(Long parentId) {
        String url = baseUrl() + "/api/categories";
        if (parentId != null) {
            url += "?parentId=" + parentId;
        }
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, LIST_OF_CATEGORY);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }
}
