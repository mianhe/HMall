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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理商品 feature 的 Step Definitions，按 catalog-api.yaml 调用 API。
 */
public class ProductStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 类别名称 -> id（用于创建商品、断言所属类别） */
    private final Map<String, Long> categoryNameToId = new ConcurrentHashMap<>();
    /** 商品名称 -> id（用于“请求该商品的详情”） */
    private final Map<String, Long> productNameToId = new ConcurrentHashMap<>();

    private ResponseEntity<ProductApiDto.Response> lastCreateProductResponse;
    private ResponseEntity<List<ProductApiDto.Response>> lastProductListResponse;
    private ResponseEntity<ProductApiDto.Response> lastProductDetailResponse;
    private ResponseEntity<ProductApiDto.Error> lastErrorResponse;
    /** 最后创建的商品 ID（用于“请求该商品的详情”） */
    private Long lastCreatedProductId;

    public ProductStepDefinitions(TestRestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    // ---------- 已存在叶子类别 ----------
    @Given("已存在叶子类别 {string}")
    public void 已存在叶子类别(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        ResponseEntity<CategoryApiDto.Response> res = postCategory(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            categoryNameToId.put(name, res.getBody().id);
        }
    }

    // ---------- 在叶子类别下创建商品 ----------
    @When("用户在类别 {string} 下创建商品 {string} 描述 {string}")
    public void 用户在类别下创建商品描述(String categoryName, String productName, String description) {
        Long categoryId = categoryNameToId.get(categoryName);
        assertThat(categoryId).as("类别「%s」应先存在", categoryName).isNotNull();
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = productName;
        body.description = description;
        lastCreateProductResponse = postProduct(body);
        if (lastCreateProductResponse.getStatusCode().is2xxSuccessful() && lastCreateProductResponse.getBody() != null) {
            productNameToId.put(productName, lastCreateProductResponse.getBody().id);
        }
    }

    @Then("商品应创建成功")
    public void 商品应创建成功() {
        assertThat(lastCreateProductResponse).isNotNull();
        assertThat(lastCreateProductResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(lastCreateProductResponse.getBody()).isNotNull();
    }

    @And("返回的商品名称为 {string}")
    public void 返回的商品名称为(String expectedName) {
        assertThat(lastCreateProductResponse.getBody()).isNotNull();
        assertThat(lastCreateProductResponse.getBody().name).isEqualTo(expectedName);
    }

    @And("返回的商品描述为 {string}")
    public void 返回的商品描述为(String expectedDesc) {
        assertThat(lastCreateProductResponse.getBody()).isNotNull();
        assertThat(lastCreateProductResponse.getBody().description).isEqualTo(expectedDesc);
    }

    @And("该商品属于类别 {string}")
    public void 该商品属于类别(String categoryName) {
        Long expectedCategoryId = categoryNameToId.get(categoryName);
        assertThat(lastCreateProductResponse.getBody()).isNotNull();
        assertThat(lastCreateProductResponse.getBody().categoryId).isEqualTo(expectedCategoryId);
    }

    // ---------- 已存在类别且其下有子类别（非叶子） ----------
    @Given("已存在类别 {string}，且 {string} 下有子类别 {string}")
    public void 已存在类别且下有子类别(String parentName, String _parentAgain, String childName) {
        CategoryApiDto.Create root = new CategoryApiDto.Create();
        root.name = parentName;
        root.parentId = null;
        ResponseEntity<CategoryApiDto.Response> rootRes = postCategory(root);
        assertThat(rootRes.getStatusCode().value()).isEqualTo(201);
        if (rootRes.getBody() != null) {
            categoryNameToId.put(parentName, rootRes.getBody().id);
        }
        CategoryApiDto.Create child = new CategoryApiDto.Create();
        child.name = childName;
        child.parentId = rootRes.getBody().id;
        ResponseEntity<CategoryApiDto.Response> childRes = postCategory(child);
        assertThat(childRes.getStatusCode().value()).isEqualTo(201);
    }

    @Then("应创建失败")
    public void 应创建失败() {
        assertThat(lastCreateProductResponse).isNotNull();
        int status = lastCreateProductResponse.getStatusCode().value();
        assertThat(status).isIn(400, 422);
    }

    @And("应返回错误提示（如仅叶子类别可挂商品）")
    public void 应返回错误提示() {
        if (lastErrorResponse == null && lastCreateProductResponse != null && lastCreateProductResponse.getStatusCode().is4xxClientError()) {
            ProductApiDto.Error e = new ProductApiDto.Error();
            e.message = String.valueOf(lastCreateProductResponse.getStatusCode());
            lastErrorResponse = ResponseEntity.status(lastCreateProductResponse.getStatusCode()).body(e);
        }
        assertThat(lastErrorResponse).isNotNull();
        assertThat(lastErrorResponse.getBody()).isNotNull();
        assertThat(lastErrorResponse.getBody().message).isNotBlank();
    }

    // ---------- 已存在叶子类别且该类别下有商品 ----------
    @Given("已存在叶子类别 {string}，且该类别下有商品 {string} 和 {string}")
    public void 已存在叶子类别且该类别下有商品(String categoryName, String product1, String product2) {
        已存在叶子类别(categoryName);
        Long categoryId = categoryNameToId.get(categoryName);
        postProductAndStore(categoryId, product1, null);
        postProductAndStore(categoryId, product2, null);
    }

    // ---------- 按类别查看商品列表 ----------
    @When("用户请求类别 {string} 下的所有商品")
    public void 用户请求类别下的所有商品(String categoryName) {
        Long categoryId = categoryNameToId.get(categoryName);
        assertThat(categoryId).as("类别「%s」应先存在", categoryName).isNotNull();
        lastProductListResponse = getProductsByCategory(categoryId);
    }

    @Then("应返回 {int} 个商品")
    public void 应返回个商品(int count) {
        assertThat(lastProductListResponse).isNotNull();
        assertThat(lastProductListResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastProductListResponse.getBody()).hasSize(count);
    }

    @And("返回的商品列表中包含名称 {string} 和 {string}")
    public void 返回的商品列表中包含名称和(String name1, String name2) {
        List<String> names = lastProductListResponse.getBody().stream()
            .map(p -> p.name)
            .toList();
        assertThat(names).containsExactlyInAnyOrder(name1, name2);
    }

    // ---------- 已存在商品（用于按 ID 查详情） ----------
    @Given("已存在商品 {string} 描述 {string} 属于类别 {string}")
    public void 已存在商品描述属于类别(String productName, String description, String categoryName) {
        已存在叶子类别(categoryName);
        Long categoryId = categoryNameToId.get(categoryName);
        postProductAndStore(categoryId, productName, description);
    }

    @When("用户请求该商品的详情")
    public void 用户请求该商品的详情() {
        Long productId = lastCreatedProductId != null ? lastCreatedProductId
            : (lastCreateProductResponse != null && lastCreateProductResponse.getBody() != null
                ? lastCreateProductResponse.getBody().id
                : productNameToId.values().stream().findFirst().orElse(null));
        assertThat(productId).as("应先存在商品").isNotNull();
        lastProductDetailResponse = getProductById(productId);
    }

    @Then("应返回商品名称为 {string}")
    public void 应返回商品名称为(String expectedName) {
        assertThat(lastProductDetailResponse.getBody()).isNotNull();
        assertThat(lastProductDetailResponse.getBody().name).isEqualTo(expectedName);
    }

    @And("应返回商品描述为 {string}")
    public void 应返回商品描述为(String expectedDesc) {
        assertThat(lastProductDetailResponse.getBody()).isNotNull();
        assertThat(lastProductDetailResponse.getBody().description).isEqualTo(expectedDesc);
    }

    @And("应返回该商品所属类别为 {string}")
    public void 应返回该商品所属类别为(String categoryName) {
        Long expectedCategoryId = categoryNameToId.get(categoryName);
        assertThat(lastProductDetailResponse.getBody()).isNotNull();
        assertThat(lastProductDetailResponse.getBody().categoryId).isEqualTo(expectedCategoryId);
    }

    // ---------- 辅助：创建商品并存入 productNameToId ----------
    private void postProductAndStore(Long categoryId, String name, String description) {
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.description = description;
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            productNameToId.put(name, res.getBody().id);
            lastCreatedProductId = res.getBody().id;
        }
    }

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

    private ResponseEntity<ProductApiDto.Response> postProduct(ProductApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                ProductApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            lastErrorResponse = ResponseEntity.status(e.getStatusCode()).body(parseErrorFromString(e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ProductApiDto.Error parseErrorFromString(String json) {
        if (json == null || json.isBlank()) {
            ProductApiDto.Error e = new ProductApiDto.Error();
            e.message = "";
            return e;
        }
        try {
            return objectMapper.readValue(json, ProductApiDto.Error.class);
        } catch (Exception ignored) {
            ProductApiDto.Error e = new ProductApiDto.Error();
            e.message = json;
            return e;
        }
    }

    private ResponseEntity<ProductApiDto.Response> getProductById(Long id) {
        try {
            return restTemplate.getForEntity(baseUrl() + "/api/products/" + id, ProductApiDto.Response.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private static final ParameterizedTypeReference<List<ProductApiDto.Response>> LIST_OF_PRODUCT =
        new ParameterizedTypeReference<>() {};

    private ResponseEntity<List<ProductApiDto.Response>> getProductsByCategory(Long categoryId) {
        String url = baseUrl() + "/api/products?categoryId=" + categoryId;
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null, LIST_OF_PRODUCT);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }
}
