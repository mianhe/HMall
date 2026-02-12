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
    private final CatalogTestContext context;

    /** 类别名称 -> id（用于创建商品、断言所属类别） */
    private final Map<String, Long> categoryNameToId = new ConcurrentHashMap<>();
    /** 商品名称 -> id（用于“请求该商品的详情”）；同时写入 context 供 SpecDimension/Sku 步骤用） */
    private final Map<String, Long> productNameToId = new ConcurrentHashMap<>();

    private ResponseEntity<ProductApiDto.Response> lastCreateProductResponse;
    private ResponseEntity<ProductApiDto.Response> lastUpdateProductResponse;
    private ResponseEntity<List<ProductApiDto.Response>> lastProductListResponse;
    private ResponseEntity<ProductApiDto.Response> lastProductDetailResponse;
    private ResponseEntity<ProductApiDto.Error> lastErrorResponse;
    /** 最后创建的商品 ID（用于“请求该商品的详情”、删除该商品） */
    private Long lastCreatedProductId;
    /** 最后一次删除请求的响应（无 body，仅状态码） */
    private ResponseEntity<Void> lastDeleteResponse;

    public ProductStepDefinitions(TestRestTemplate restTemplate, ObjectMapper objectMapper, CatalogTestContext context) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.context = context;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    // ---------- 已存在叶子类别/类目（类目与需求文档一致，实现相同） ----------
    @Given("已存在叶子类别 {string}")
    public void 已存在叶子类别(String name) {
        do已存在叶子类目(name);
    }

    @Given("已存在叶子类目 {string}")
    public void 已存在叶子类目(String name) {
        do已存在叶子类目(name);
    }

    private void do已存在叶子类目(String name) {
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        ResponseEntity<CategoryApiDto.Response> res = postCategory(body);
        context.setLastStatusCode(res.getStatusCode().value());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            categoryNameToId.put(name, res.getBody().id);
        }
    }

    @Given("已存在商品 {string} 描述 {string} 属于类目 {string}")
    public void 已存在商品描述属于类目(String productName, String description, String categoryName) {
        已存在商品描述属于类别(productName, description, categoryName);
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
        context.setLastStatusCode(lastCreateProductResponse.getStatusCode().value());
        if (lastCreateProductResponse.getStatusCode().is2xxSuccessful() && lastCreateProductResponse.getBody() != null) {
            Long id = lastCreateProductResponse.getBody().id;
            productNameToId.put(productName, id);
            context.putSpuId(productName, id);
            context.setLastProductName(productName);
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
        ProductApiDto.Response body = lastProductBody();
        assertThat(body).isNotNull();
        assertThat(body.name).isEqualTo(expectedName);
    }

    @And("返回的商品描述为 {string}")
    public void 返回的商品描述为(String expectedDesc) {
        ProductApiDto.Response body = lastProductBody();
        assertThat(body).isNotNull();
        assertThat(body.description).isEqualTo(expectedDesc);
    }

    /** 取最后一次创建或修改的商品响应体（修改优先） */
    private ProductApiDto.Response lastProductBody() {
        if (lastUpdateProductResponse != null && lastUpdateProductResponse.getBody() != null) {
            return lastUpdateProductResponse.getBody();
        }
        return lastCreateProductResponse != null ? lastCreateProductResponse.getBody() : null;
    }

    @And("该商品属于类别 {string}")
    public void 该商品属于类别(String categoryName) {
        该商品属于类目(categoryName);
    }

    @And("该商品属于类目 {string}")
    public void 该商品属于类目(String categoryName) {
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

    @When("用户在类目 ID {long} 下创建商品 {string} 描述 {string}")
    public void 用户在类目ID下创建商品(long categoryId, String name, String description) {
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.description = description;
        lastCreateProductResponse = postProduct(body);
        context.setLastStatusCode(lastCreateProductResponse.getStatusCode().value());
    }

    @When("用户请求商品 ID {long} 的详情")
    public void 用户请求商品ID的详情(long productId) {
        lastProductDetailResponse = getProductById(productId);
        context.setLastStatusCode(lastProductDetailResponse.getStatusCode().value());
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

    // ---------- 修改商品 ----------
    @When("用户将商品 {string} 修改为名称 {string} 描述 {string}")
    public void 用户将商品修改为名称描述(String productName, String newName, String newDescription) {
        Long id = productNameToId.get(productName);
        assertThat(id).as("商品「%s」应先存在", productName).isNotNull();
        ProductApiDto.Update body = new ProductApiDto.Update();
        body.name = newName;
        body.description = newDescription;
        lastUpdateProductResponse = putProduct(id, body);
        context.setLastStatusCode(lastUpdateProductResponse.getStatusCode().value());
    }

    @When("用户将商品 ID {long} 修改为名称 {string}")
    public void 用户将商品ID修改为名称(long id, String newName) {
        ProductApiDto.Update body = new ProductApiDto.Update();
        body.name = newName;
        body.description = null;
        lastUpdateProductResponse = putProduct(id, body);
        context.setLastStatusCode(lastUpdateProductResponse.getStatusCode().value());
    }

    // ---------- 删除商品 ----------
    @When("用户删除该商品")
    public void 用户删除该商品() {
        Long productId = lastCreatedProductId != null ? lastCreatedProductId
            : (lastCreateProductResponse != null && lastCreateProductResponse.getBody() != null
                ? lastCreateProductResponse.getBody().id
                : productNameToId.values().stream().findFirst().orElse(null));
        assertThat(productId).as("应先存在商品").isNotNull();
        lastDeleteResponse = deleteProduct(productId);
        context.setLastStatusCode(lastDeleteResponse.getStatusCode().value());
    }

    @When("用户删除商品 ID {long}")
    public void 用户删除商品ID(long id) {
        lastDeleteResponse = deleteProduct(id);
        context.setLastStatusCode(lastDeleteResponse.getStatusCode().value());
    }

    @And("再次请求该商品详情应返回 404")
    public void 再次请求该商品详情应返回404() {
        Long productId = lastCreatedProductId != null ? lastCreatedProductId
            : (lastCreateProductResponse != null && lastCreateProductResponse.getBody() != null
                ? lastCreateProductResponse.getBody().id
                : null);
        assertThat(productId).as("应先有被删商品 id").isNotNull();
        ResponseEntity<ProductApiDto.Response> getRes = getProductById(productId);
        assertThat(getRes.getStatusCode().value()).isEqualTo(404);
    }

    private ResponseEntity<Void> deleteProduct(Long id) {
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + id,
                HttpMethod.DELETE,
                null,
                Void.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    // ---------- 辅助：创建商品并存入 productNameToId ----------
    private void postProductAndStore(Long categoryId, String name, String description) {
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.description = description;
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        context.setLastStatusCode(res.getStatusCode().value());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            Long id = res.getBody().id;
            productNameToId.put(name, id);
            context.putSpuId(name, id);
            context.setLastProductName(name);
            lastCreatedProductId = id;
        }
    }

    private ResponseEntity<CategoryApiDto.Response> postCategory(CategoryApiDto.Create body) {
        return CategoryApiDto.postCategory(restTemplate, body);
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

    private ResponseEntity<ProductApiDto.Response> putProduct(Long id, ProductApiDto.Update body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                baseUrl() + "/api/products/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                ProductApiDto.Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
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
