package com.hmall.catalog.acceptance;

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

public class ServiceBindingStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CatalogTestContext context;

    private final Map<String, Long> productNameToId = new ConcurrentHashMap<>();
    private final Map<String, Long> skuNameToId = new ConcurrentHashMap<>();
    private final Map<String, Long> bindingKeyToId = new ConcurrentHashMap<>();
    private final Map<String, Long> dimensionCache = new ConcurrentHashMap<>();
    private final Map<String, Long> optionCache = new ConcurrentHashMap<>();
    private final Map<String, Long> categoryNameToId = new ConcurrentHashMap<>();

    private ResponseEntity<ServiceBindingApiDto.Response> lastBindingResponse;
    private ResponseEntity<List<ServiceBindingApiDto.AvailableService>> lastAvailableServicesResponse;
    private ResponseEntity<ProductApiDto.Response> lastProductCreateResponse;
    private ResponseEntity<ProductApiDto.Response> lastProductDetailResponse;

    public ServiceBindingStepDefinitions(TestRestTemplate restTemplate, CatalogTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    // ---------- Given: 创建实体/服务商品 ----------

    @Given("已存在实体商品 {string} 属于类目 {string}")
    public void 已存在实体商品属于类目(String name, String categoryName) {
        Long categoryId = ensureCategory(categoryName);
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.productType = "PHYSICAL";
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        productNameToId.put(name, res.getBody().id);
        context.putSpuId(name, res.getBody().id);
        context.setLastProductName(name);
    }

    @Given("已存在服务商品 {string} 属于类目 {string}")
    public void 已存在服务商品属于类目(String name, String categoryName) {
        Long categoryId = ensureCategory(categoryName);
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.productType = "SERVICE";
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        productNameToId.put(name, res.getBody().id);
        context.putSpuId(name, res.getBody().id);
        context.setLastProductName(name);
    }

    @Given("服务商品 {string} 有规格维度 {string} 选项 {string}")
    public void 服务商品有规格维度选项(String productName, String dimensionName, String optionValue) {
        Long spuId = resolveSpuId(productName);
        String dimKey = productName + "::" + dimensionName;
        Long dimensionId = dimensionCache.get(dimKey);
        if (dimensionId == null) {
            dimensionId = createDimension(spuId, dimensionName);
            dimensionCache.put(dimKey, dimensionId);
        }
        Long optionId = createOption(spuId, dimensionId, optionValue);
        optionCache.put(productName + "::" + dimensionName + ":" + optionValue, optionId);
    }

    @Given("服务商品 {string} 有 SKU 选择 {string} 命名为 {string}")
    public void 服务商品有SKU选择命名为(String productName, String specSelection, String skuName) {
        Long spuId = resolveSpuId(productName);
        Long optionId = resolveOptionId(productName, specSelection);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = List.of(optionId);
        body.priceCents = 0L;
        body.displayName = skuName;
        ResponseEntity<SkuApiDto.Response> res = postSku(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        skuNameToId.put(skuName, res.getBody().id);
    }

    @Given("实体商品 {string} 有 SKU 价格 {long} 分 命名为 {string}")
    public void 实体商品有SKU价格命名为(String productName, long priceCents, String skuName) {
        Long spuId = resolveSpuId(productName);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = List.of();
        body.priceCents = priceCents;
        body.displayName = skuName;
        ResponseEntity<SkuApiDto.Response> res = postSku(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        skuNameToId.put(skuName, res.getBody().id);
    }

    @Given("服务 SKU {string} 已绑定到实体商品 {string} 售价 {long} 分")
    public void 服务SKU已绑定到实体商品售价(String skuName, String targetName, long priceCents) {
        Long skuId = resolveSkuId(skuName);
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = priceCents;
        ResponseEntity<ServiceBindingApiDto.Response> res = postBinding(skuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        bindingKeyToId.put(skuName + "->" + targetName, res.getBody().id);
    }

    @Given("服务 SKU {string} 已绑定到实体商品 {string} 不指定售价")
    public void 服务SKU已绑定到实体商品不指定售价(String skuName, String targetName) {
        Long skuId = resolveSkuId(skuName);
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = null;
        ResponseEntity<ServiceBindingApiDto.Response> res = postBinding(skuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        bindingKeyToId.put(skuName + "->" + targetName, res.getBody().id);
    }

    @Given("服务商品 {string} 有 SKU 价格 {long} 分 命名为 {string}")
    public void 服务商品有SKU价格命名为(String productName, long priceCents, String skuName) {
        Long spuId = resolveSpuId(productName);
        SkuApiDto.Create body = new SkuApiDto.Create();
        body.specOptionIds = List.of();
        body.priceCents = priceCents;
        body.displayName = skuName;
        ResponseEntity<SkuApiDto.Response> res = postSku(spuId, body);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        skuNameToId.put(skuName, res.getBody().id);
    }

    // ---------- When: 创建商品（含 productType） ----------

    @When("用户在类目 {string} 下创建商品 {string} 描述 {string} 商品类型 {string}")
    public void 用户在类目下创建商品描述商品类型(String categoryName, String name, String desc, String productType) {
        Long categoryId = ensureCategory(categoryName);
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.description = desc;
        body.productType = productType;
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        context.setLastStatusCode(res.getStatusCode().value());
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            productNameToId.put(name, res.getBody().id);
            context.putSpuId(name, res.getBody().id);
            context.setLastProductName(name);
        }
        lastProductCreateResponse = res;
    }

    @When("用户在类目 {string} 下创建服务商品 {string} 描述 {string}")
    public void 用户在类目下创建服务商品(String categoryName, String name, String desc) {
        Long categoryId = ensureCategory(categoryName);
        ProductApiDto.Create body = new ProductApiDto.Create();
        body.categoryId = categoryId;
        body.name = name;
        body.description = desc;
        body.productType = "SERVICE";
        ResponseEntity<ProductApiDto.Response> res = postProduct(body);
        context.setLastStatusCode(res.getStatusCode().value());
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            productNameToId.put(name, res.getBody().id);
            context.putSpuId(name, res.getBody().id);
            context.setLastProductName(name);
        }
        lastProductCreateResponse = res;
    }

    // ---------- When: ServiceBinding 操作（SKU 级） ----------

    @When("用户为服务 SKU {string} 添加 ServiceBinding 关联到 {string} 售价 {long} 分")
    public void 用户为服务SKU添加ServiceBinding关联到售价(String skuName, String targetName, long priceCents) {
        Long skuId = resolveSkuId(skuName);
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = priceCents;
        lastBindingResponse = postBinding(skuId, body);
        context.setLastStatusCode(lastBindingResponse.getStatusCode().value());
        if (lastBindingResponse.getStatusCode().is2xxSuccessful() && lastBindingResponse.getBody() != null) {
            bindingKeyToId.put(skuName + "->" + targetName, lastBindingResponse.getBody().id);
        }
    }

    @When("用户为服务 SKU {string} 添加 ServiceBinding 关联到 {string} 不指定售价")
    public void 用户为服务SKU添加ServiceBinding关联到不指定售价(String skuName, String targetName) {
        Long skuId = resolveSkuId(skuName);
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = null;
        lastBindingResponse = postBinding(skuId, body);
        context.setLastStatusCode(lastBindingResponse.getStatusCode().value());
        if (lastBindingResponse.getStatusCode().is2xxSuccessful() && lastBindingResponse.getBody() != null) {
            bindingKeyToId.put(skuName + "->" + targetName, lastBindingResponse.getBody().id);
        }
    }

    @When("用户为服务 SKU {string} 添加 ServiceBinding 关联到不存在的 SPU 售价 {long} 分")
    public void 用户为服务SKU添加ServiceBinding关联到不存在SPU售价(String skuName, long priceCents) {
        Long skuId = resolveSkuId(skuName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = 999999L;
        body.priceCents = priceCents;
        lastBindingResponse = postBinding(skuId, body);
        context.setLastStatusCode(lastBindingResponse.getStatusCode().value());
    }

    @When("用户为不存在的服务 SKU 添加 ServiceBinding 关联到 {string} 售价 {long} 分")
    public void 用户为不存在的服务SKU添加ServiceBinding关联到售价(String targetName, long priceCents) {
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = priceCents;
        lastBindingResponse = postBinding(999999L, body);
        context.setLastStatusCode(lastBindingResponse.getStatusCode().value());
    }

    @When("用户为实体 SKU {string} 添加 ServiceBinding 关联到 {string} 售价 {long} 分")
    public void 用户为实体SKU添加ServiceBinding关联到售价(String skuName, String targetName, long priceCents) {
        Long skuId = resolveSkuId(skuName);
        Long targetSpuId = resolveSpuId(targetName);
        ServiceBindingApiDto.Create body = new ServiceBindingApiDto.Create();
        body.targetSpuId = targetSpuId;
        body.priceCents = priceCents;
        lastBindingResponse = postBinding(skuId, body);
        context.setLastStatusCode(lastBindingResponse.getStatusCode().value());
    }

    @When("用户查询实体商品 {string} 的可选服务列表")
    public void 用户查询实体商品的可选服务列表(String targetName) {
        Long targetSpuId = resolveSpuId(targetName);
        lastAvailableServicesResponse = getAvailableServices(targetSpuId);
        context.setLastStatusCode(lastAvailableServicesResponse.getStatusCode().value());
    }

    @When("用户删除服务 SKU {string} 与实体商品 {string} 的 ServiceBinding")
    public void 用户删除ServiceBinding(String skuName, String targetName) {
        Long skuId = resolveSkuId(skuName);
        Long bindingId = bindingKeyToId.get(skuName + "->" + targetName);
        assertThat(bindingId).as("ServiceBinding「%s->%s」应先存在", skuName, targetName).isNotNull();
        ResponseEntity<Void> res = deleteBinding(skuId, bindingId);
        context.setLastStatusCode(res.getStatusCode().value());
    }

    @When("用户查询商品 {string} 的详情")
    public void 用户查询商品的详情ByName(String productName) {
        Long spuId = resolveSpuId(productName);
        lastProductDetailResponse = getProductById(spuId);
        context.setLastStatusCode(lastProductDetailResponse.getStatusCode().value());
    }

    // ---------- Then: ServiceBinding ----------

    @Then("ServiceBinding 应创建成功")
    public void serviceBinding应创建成功() {
        assertThat(lastBindingResponse).isNotNull();
        assertThat(lastBindingResponse.getStatusCode().value()).isEqualTo(201);
    }

    @And("返回的 ServiceBinding 售价为 {long} 分")
    public void 返回的ServiceBinding售价为(long expectedPrice) {
        assertThat(lastBindingResponse).isNotNull();
        assertThat(lastBindingResponse.getBody()).isNotNull();
        assertThat(lastBindingResponse.getBody().priceCents).isEqualTo(expectedPrice);
    }

    @And("返回的 ServiceBinding 售价为空")
    public void 返回的ServiceBinding售价为空() {
        assertThat(lastBindingResponse).isNotNull();
        assertThat(lastBindingResponse.getBody()).isNotNull();
        assertThat(lastBindingResponse.getBody().priceCents).isNull();
    }

    @Then("应返回 {int} 个可选服务")
    public void 应返回个可选服务(int count) {
        assertThat(lastAvailableServicesResponse).isNotNull();
        assertThat(lastAvailableServicesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(lastAvailableServicesResponse.getBody()).hasSize(count);
    }

    @And("可选服务 {string} 包含 {int} 个绑定")
    public void 可选服务包含个绑定(String serviceName, int count) {
        ServiceBindingApiDto.AvailableService svc = findAvailableService(serviceName);
        assertThat(svc.bindings).as("服务「%s」的绑定数量", serviceName).hasSize(count);
    }

    @And("可选服务 {string} 包含规格 {string} 售价 {long} 分")
    public void 可选服务包含规格售价(String serviceName, String specExpr, long expectedPrice) {
        ServiceBindingApiDto.AvailableService svc = findAvailableService(serviceName);
        String[] parts = specExpr.split(":");
        String dimName = parts[0];
        String optValue = parts[1];
        assertThat(svc.bindings).anyMatch(b ->
                b.priceCents == expectedPrice
                        && b.specValues != null
                        && b.specValues.stream().anyMatch(sv ->
                        dimName.equals(sv.dimensionName) && optValue.equals(sv.optionValue)));
    }

    @And("可选服务 {string} 的第一个绑定售价为 {long} 分")
    public void 可选服务的第一个绑定售价为(String serviceName, long expectedPrice) {
        ServiceBindingApiDto.AvailableService svc = findAvailableService(serviceName);
        assertThat(svc.bindings).as("服务「%s」应至少有一个绑定", serviceName).isNotEmpty();
        assertThat(svc.bindings.get(0).priceCents).as("第一个绑定的售价").isEqualTo(expectedPrice);
    }

    @And("查询实体商品 {string} 的可选服务列表应返回 {int} 个服务")
    public void 查询实体商品的可选服务列表应返回个服务(String targetName, int count) {
        Long targetSpuId = resolveSpuId(targetName);
        ResponseEntity<List<ServiceBindingApiDto.AvailableService>> res = getAvailableServices(targetSpuId);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).hasSize(count);
    }

    // ---------- Then: productType 相关断言 ----------

    @And("返回的 productType 为 {string}")
    public void 返回的productType为(String expectedType) {
        ProductApiDto.Response body = lastProductBody();
        assertThat(body).isNotNull();
        assertThat(body.productType).isEqualTo(expectedType);
    }

    @And("搜索结果中商品 {string} 的 productType 为 {string}")
    public void 搜索结果中商品的productType为(String name, String expectedType) {
        ResponseEntity<List<ProductApiDto.Response>> res = searchProducts("");
        assertThat(res.getBody()).isNotNull();
        ProductApiDto.Response match = res.getBody().stream()
                .filter(p -> name.equals(p.name))
                .findFirst()
                .orElse(null);
        assertThat(match).as("搜索结果中应包含商品「%s」", name).isNotNull();
        assertThat(match.productType).isEqualTo(expectedType);
    }

    private ProductApiDto.Response lastProductBody() {
        if (lastProductDetailResponse != null && lastProductDetailResponse.getBody() != null) {
            return lastProductDetailResponse.getBody();
        }
        if (lastProductCreateResponse != null && lastProductCreateResponse.getBody() != null) {
            return lastProductCreateResponse.getBody();
        }
        String productName = context.getLastProductName();
        if (productName != null) {
            Long spuId = context.getSpuId(productName);
            if (spuId != null) {
                ResponseEntity<ProductApiDto.Response> res = getProductById(spuId);
                if (res.getStatusCode().is2xxSuccessful()) {
                    return res.getBody();
                }
            }
        }
        return null;
    }

    // ---------- 辅助方法 ----------

    private Long ensureCategory(String name) {
        if (categoryNameToId.containsKey(name)) {
            return categoryNameToId.get(name);
        }
        CategoryApiDto.Create body = new CategoryApiDto.Create();
        body.name = name;
        body.parentId = null;
        ResponseEntity<CategoryApiDto.Response> res = CategoryApiDto.postCategory(restTemplate, body);
        if (res.getStatusCode().value() == 201 && res.getBody() != null) {
            categoryNameToId.put(name, res.getBody().id);
            return res.getBody().id;
        }
        throw new AssertionError("创建类目「" + name + "」失败: " + res.getStatusCode());
    }

    private Long resolveSpuId(String productName) {
        Long id = productNameToId.get(productName);
        if (id == null) {
            id = context.getSpuId(productName);
        }
        assertThat(id).as("商品「%s」应先存在", productName).isNotNull();
        return id;
    }

    private Long resolveSkuId(String skuName) {
        Long id = skuNameToId.get(skuName);
        assertThat(id).as("SKU「%s」应先存在", skuName).isNotNull();
        return id;
    }

    private Long resolveOptionId(String productName, String specSelection) {
        String key = productName + "::" + specSelection;
        Long id = optionCache.get(key);
        assertThat(id).as("SpecOption「%s::%s」应先存在", productName, specSelection).isNotNull();
        return id;
    }

    private ServiceBindingApiDto.AvailableService findAvailableService(String serviceName) {
        assertThat(lastAvailableServicesResponse).isNotNull();
        assertThat(lastAvailableServicesResponse.getBody()).isNotNull();
        return lastAvailableServicesResponse.getBody().stream()
                .filter(s -> serviceName.equals(s.name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("可选服务列表中未找到「" + serviceName + "」"));
    }

    private ResponseEntity<ProductApiDto.Response> postProduct(ProductApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                    baseUrl() + "/api/products", HttpMethod.POST,
                    new HttpEntity<>(body, headers), ProductApiDto.Response.class);
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

    private ResponseEntity<List<ProductApiDto.Response>> searchProducts(String keyword) {
        String url = baseUrl() + "/api/products/search";
        if (keyword != null && !keyword.isEmpty()) {
            url += "?keyword=" + keyword;
        }
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<ProductApiDto.Response>>() {});
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private Long createDimension(Long spuId, String dimensionName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("name", dimensionName, "required", true);
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    baseUrl() + "/api/products/" + spuId + "/dimensions",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            assertThat(res.getStatusCode().value()).isEqualTo(201);
            return ((Number) res.getBody().get("id")).longValue();
        } catch (RestClientResponseException e) {
            throw new AssertionError("创建规格维度失败: " + e.getResponseBodyAsString(), e);
        }
    }

    private Long createOption(Long spuId, Long dimensionId, String optionValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("optionValue", optionValue);
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    baseUrl() + "/api/products/" + spuId + "/dimensions/" + dimensionId + "/options",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            assertThat(res.getStatusCode().value()).isEqualTo(201);
            return ((Number) res.getBody().get("id")).longValue();
        } catch (RestClientResponseException e) {
            throw new AssertionError("创建规格选项失败: " + e.getResponseBodyAsString(), e);
        }
    }

    private ResponseEntity<SkuApiDto.Response> postSku(Long spuId, SkuApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                    baseUrl() + "/api/products/" + spuId + "/skus",
                    HttpMethod.POST, new HttpEntity<>(body, headers), SkuApiDto.Response.class);
        } catch (RestClientResponseException e) {
            throw new AssertionError("创建 SKU 失败: " + e.getResponseBodyAsString(), e);
        }
    }

    private ResponseEntity<ServiceBindingApiDto.Response> postBinding(Long skuId, ServiceBindingApiDto.Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                    baseUrl() + "/api/skus/" + skuId + "/service-bindings",
                    HttpMethod.POST, new HttpEntity<>(body, headers),
                    ServiceBindingApiDto.Response.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<List<ServiceBindingApiDto.AvailableService>> getAvailableServices(Long targetSpuId) {
        try {
            return restTemplate.exchange(
                    baseUrl() + "/api/products/" + targetSpuId + "/available-services",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<ServiceBindingApiDto.AvailableService>>() {});
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private ResponseEntity<Void> deleteBinding(Long skuId, Long bindingId) {
        try {
            return restTemplate.exchange(
                    baseUrl() + "/api/skus/" + skuId + "/service-bindings/" + bindingId,
                    HttpMethod.DELETE, null, Void.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }
}
