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

    /** 最后一次单条类别响应（创建 / 修改 / 查详情共用） */
    private ResponseEntity<CategoryApiDto.Response> lastCategoryResponse;
    private ResponseEntity<List<CategoryApiDto.Response>> lastListResponse;

    public CategoryStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    private void setContextStatus(ResponseEntity<?> res) {
        if (context != null && res != null) {
            context.setLastStatusCode(res.getStatusCode().value());
        }
    }

    // ---------- 创建根级类别 ----------
    @When("用户创建根级类别 {string}")
    public void 用户创建根级类别(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        lastCategoryResponse = postCategory(body);
        setContextStatus(lastCategoryResponse);
        if (lastCategoryResponse.getStatusCode().is2xxSuccessful() && lastCategoryResponse.getBody() != null) {
            categoryNameToId.put(name, lastCategoryResponse.getBody().id);
        }
    }

    @And("返回的类别名称为 {string}")
    public void 返回的类别名称为(String expectedName) {
        assertThat(lastCategoryResponse.getBody()).isNotNull();
        assertThat(lastCategoryResponse.getBody().name).isEqualTo(expectedName);
    }

    @And("该类别无父类别")
    public void 该类别无父类别() {
        assertThat(lastCategoryResponse.getBody()).isNotNull();
        assertThat(lastCategoryResponse.getBody().parentId).isNull();
    }

    // ---------- 已存在根级类别（Given） ----------
    @Given("已存在根级类别 {string}")
    public void 已存在根级类别(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        ResponseEntity<CategoryApiDto.Response> res = postCategory(body);
        setContextStatus(res);
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
        lastCategoryResponse = postCategory(body);
        setContextStatus(lastCategoryResponse);
        if (lastCategoryResponse.getStatusCode().is2xxSuccessful() && lastCategoryResponse.getBody() != null) {
            categoryNameToId.put(childName, lastCategoryResponse.getBody().id);
        }
    }

    @When("用户在类别 {string} 下创建子类别 {string}")
    public void 用户在类别下创建子类别(String parentName, String childName) {
        Long parentId = categoryNameToId.get(parentName);
        assertThat(parentId).as("类别「%s」应先存在", parentName).isNotNull();
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = childName;
        body.parentId = parentId;
        lastCategoryResponse = postCategory(body);
        setContextStatus(lastCategoryResponse);
        if (lastCategoryResponse.getStatusCode().is2xxSuccessful() && lastCategoryResponse.getBody() != null) {
            categoryNameToId.put(childName, lastCategoryResponse.getBody().id);
        }
    }

    @And("该类别的父类别为 {string}")
    public void 该类别的父类别为(String parentName) {
        Long expectedParentId = categoryNameToId.get(parentName);
        assertThat(lastCategoryResponse.getBody()).isNotNull();
        assertThat(lastCategoryResponse.getBody().parentId).isEqualTo(expectedParentId);
    }

    @And("返回的类别描述为 {string}")
    public void 返回的类别描述为(String expectedDescription) {
        assertThat(lastCategoryResponse.getBody()).isNotNull();
        assertThat(lastCategoryResponse.getBody().description).isEqualTo(expectedDescription);
    }

    // ---------- 修改类别 ----------
    @When("用户将类别 {string} 修改为名称 {string} 描述 {string}")
    public void 用户将类别修改为名称描述(String categoryName, String newName, String newDescription) {
        Long id = categoryNameToId.get(categoryName);
        assertThat(id).as("类别「%s」应先存在", categoryName).isNotNull();
        CategoryApiDto.Update body = new CategoryApiDto.Update();
        body.name = newName;
        body.description = newDescription;
        lastCategoryResponse = putCategory(id, body);
        setContextStatus(lastCategoryResponse);
    }

    @When("用户将类目 ID {long} 修改为名称 {string}")
    public void 用户将类目ID修改为名称(long id, String newName) {
        CategoryApiDto.Update body = new CategoryApiDto.Update();
        body.name = newName;
        body.description = null;
        lastCategoryResponse = putCategory(id, body);
        setContextStatus(lastCategoryResponse);
    }

    // ---------- 删除类别 ----------
    @When("用户删除类别 {string}")
    public void 用户删除类别(String categoryName) {
        Long id = categoryNameToId.get(categoryName);
        assertThat(id).as("类别「%s」应先存在", categoryName).isNotNull();
        ResponseEntity<Void> res = deleteCategory(id);
        setContextStatus(res);
    }

    @When("用户删除类目 ID {long}")
    public void 用户删除类目ID(long id) {
        ResponseEntity<Void> res = deleteCategory(id);
        setContextStatus(res);
    }

    // ---------- 按 ID 查类别详情 ----------
    @When("用户请求类别 {string} 的详情")
    public void 用户请求类别详情(String categoryName) {
        Long id = categoryNameToId.get(categoryName);
        assertThat(id).as("类别「%s」应先存在（或曾存在）", categoryName).isNotNull();
        lastCategoryResponse = getCategoryById(id);
        setContextStatus(lastCategoryResponse);
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
        return CategoryApiDto.postCategory(restTemplate, body);
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

    private ResponseEntity<CategoryApiDto.Response> getCategoryById(Long id) {
        String url = baseUrl() + "/api/categories/" + id;
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, CategoryApiDto.Response.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<CategoryApiDto.Response> putCategory(Long id, CategoryApiDto.Update body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/categories/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                CategoryApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<Void> deleteCategory(Long id) {
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/categories/" + id,
                HttpMethod.DELETE,
                null,
                Void.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }
}
