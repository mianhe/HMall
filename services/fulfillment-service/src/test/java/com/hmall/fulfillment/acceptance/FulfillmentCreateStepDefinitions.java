package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.FulfillmentType;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
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

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentCreateStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final EventCapture eventCapture;
    private final FulfillmentOrderRepository repository;

    public FulfillmentCreateStepDefinitions(TestRestTemplate restTemplate,
                                            FulfillmentTestContext context,
                                            EventCapture eventCapture,
                                            FulfillmentOrderRepository repository) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.eventCapture = eventCapture;
        this.repository = repository;
    }

    @当("Order 调用创建履约单接口 orderId {long} items 为 skuId {long} 数量 {int}、skuId {long} 数量 {int} 收货地址 {string} {string} {string} {string} {string} {string}")
    public void order调用创建履约单两个items(long orderId, long sku1, int qty1, long sku2, int qty2,
                                              String name, String phone, String province, String city,
                                              String district, String detail) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(
                Map.of("skuId", sku1, "quantity", qty1, "itemType", "PHYSICAL"),
                Map.of("skuId", sku2, "quantity", qty2, "itemType", "PHYSICAL")
            ),
            "shippingAddress", Map.of(
                "recipientName", name, "phone", phone,
                "province", province, "city", city, "district", district, "detail", detail
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口 orderId {long} items 为 skuId {long} 数量 {int} 收货地址 {string} {string} {string} {string} {string} {string}")
    public void order调用创建履约单单个item(long orderId, long skuId, int qty,
                                              String name, String phone, String province, String city,
                                              String district, String detail) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(Map.of("skuId", skuId, "quantity", qty, "itemType", "PHYSICAL")),
            "shippingAddress", Map.of(
                "recipientName", name, "phone", phone,
                "province", province, "city", city, "district", district, "detail", detail
            )
        );
        postCreate(body);
        if (context.getFirstFulfillmentOrderIds() == null && context.getLastResponseBody() != null) {
            context.setFirstFulfillmentOrderIds(extractFulfillmentOrderIds(context.getLastResponseBody()));
        }
    }

    @当("Order 再次调用创建履约单接口 orderId {long} items 为 skuId {long} 数量 {int} 收货地址 {string} {string} {string} {string} {string} {string}")
    public void order再次调用创建履约单(long orderId, long skuId, int qty,
                                          String name, String phone, String province, String city,
                                          String district, String detail) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(Map.of("skuId", skuId, "quantity", qty, "itemType", "PHYSICAL")),
            "shippingAddress", Map.of(
                "recipientName", name, "phone", phone,
                "province", province, "city", city, "district", district, "detail", detail
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口且缺少 orderId")
    public void order调用创建履约单缺少orderId() {
        Map<String, Object> body = Map.of(
            "items", List.of(Map.of("skuId", 1001, "quantity", 1, "itemType", "PHYSICAL")),
            "shippingAddress", Map.of(
                "recipientName", "测试", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口且 items 为空")
    public void order调用创建履约单items为空() {
        Map<String, Object> body = Map.of(
            "orderId", 1L,
            "items", List.of(),
            "shippingAddress", Map.of(
                "recipientName", "测试", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口 orderId {long} 含 1 个实体商品 skuId {long} 和 1 个服务商品 skuId {long}")
    public void order调用创建履约单含实体和服务(long orderId, long physicalSkuId, long serviceSkuId) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(
                Map.of("skuId", physicalSkuId, "quantity", 1, "itemType", "PHYSICAL"),
                Map.of("skuId", serviceSkuId, "quantity", 1, "itemType", "SERVICE")
            ),
            "shippingAddress", Map.of(
                "recipientName", "测试", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口 orderId {long} 含 1 个实体商品 skuId {long} 和 1 个镭雕服务 skuId {long} 关联 skuId {long} 雕刻 图案 {long} 名称 {string} 文字 {string}")
    public void order调用创建履约单含镭雕(long orderId, long physicalSkuId, long engravingSkuId, long relatedSkuId, long patternId, String patternName, String text) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(
                Map.of("skuId", physicalSkuId, "quantity", 1, "itemType", "PHYSICAL"),
                Map.of("skuId", engravingSkuId, "quantity", 1, "itemType", "SERVICE", "relatedSkuId", relatedSkuId,
                    "serviceAttributes", Map.of("engravingPatternId", patternId, "engravingPatternName", patternName, "engravingText", text))
            ),
            "shippingAddress", Map.of(
                "recipientName", "测试", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        postCreate(body);
    }

    @当("Order 调用创建履约单接口 orderId {long} 仅含服务商品 skuId {long}")
    public void order调用创建履约单仅含服务(long orderId, long serviceSkuId) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(Map.of("skuId", serviceSkuId, "quantity", 1, "itemType", "SERVICE")),
            "shippingAddress", Map.of(
                "recipientName", "测试", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        postCreate(body);
    }

    @并且("返回结果包含 orderId {long}")
    public void 返回结果包含orderId(long orderId) {
        assertThat(context.getLastResponseBody()).isNotNull();
        assertThat(((Number) context.getLastResponseBody().get("orderId")).longValue()).isEqualTo(orderId);
    }

    @并且("返回结果包含 fulfillmentOrderIds 列表且非空")
    public void 返回结果包含fulfillmentOrderIds() {
        assertThat(context.getLastResponseBody()).isNotNull();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) context.getLastResponseBody().get("fulfillmentOrderIds");
        assertThat(ids).isNotNull().isNotEmpty();
    }

    @并且("返回结果包含 {int} 个 fulfillmentOrderId")
    public void 返回结果包含N个fulfillmentOrderId(int expectedCount) {
        assertThat(context.getLastResponseBody()).isNotNull();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) context.getLastResponseBody().get("fulfillmentOrderIds");
        assertThat(ids).isNotNull().hasSize(expectedCount);
    }

    @并且("应发布 FulfillmentOrderCreated 事件且 orderId 为 {long}")
    public void 应发布FulfillmentOrderCreated事件(long orderId) {
        List<FulfillmentOrderCreated> events = eventCapture.getCreatedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    @并且("该订单应包含 1 个 PHYSICAL 履约单和 1 个 VIRTUAL 履约单")
    public void 该订单应包含两个类型履约单() {
        Long orderId = ((Number) context.getLastResponseBody().get("orderId")).longValue();
        var orders = repository.findByOrderId(orderId);
        assertThat(orders).hasSize(2);
        assertThat(orders.stream().filter(o -> o.getFulfillmentType() == FulfillmentType.PHYSICAL).count()).isEqualTo(1);
        assertThat(orders.stream().filter(o -> o.getFulfillmentType() == FulfillmentType.VIRTUAL).count()).isEqualTo(1);
    }

    @并且("该订单应仅包含 1 个 PHYSICAL 履约单")
    public void 该订单应仅包含1个PHYSICAL履约单() {
        Long orderId = ((Number) context.getLastResponseBody().get("orderId")).longValue();
        var orders = repository.findByOrderId(orderId);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getFulfillmentType()).isEqualTo(FulfillmentType.PHYSICAL);
    }

    @并且("该 PHYSICAL 履约单应含 engravingInfo 图案 {long} 名称 {string} 文字 {string}")
    public void 该PHYSICAL履约单应含engravingInfo(long patternId, String patternName, String text) {
        Long orderId = ((Number) context.getLastResponseBody().get("orderId")).longValue();
        var orders = repository.findByOrderId(orderId);
        assertThat(orders).hasSize(1);
        var fo = orders.get(0);
        assertThat(fo.getEngravingInfo()).isNotNull();
        assertThat(fo.getEngravingInfo().getPatternId()).isEqualTo(patternId);
        assertThat(fo.getEngravingInfo().getPatternName()).isEqualTo(patternName);
        assertThat(fo.getEngravingInfo().getText()).isEqualTo(text);
    }

    @并且("该订单应仅包含 VIRTUAL 履约单")
    public void 该订单应仅包含virtual履约单() {
        Long orderId = ((Number) context.getLastResponseBody().get("orderId")).longValue();
        var orders = repository.findByOrderId(orderId);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getFulfillmentType()).isEqualTo(FulfillmentType.VIRTUAL);
    }

    @并且("虚拟履约单状态应为 ACTIVATED")
    public void 虚拟履约单状态应为ACTIVATED() {
        Long orderId = ((Number) context.getLastResponseBody().get("orderId")).longValue();
        var orders = repository.findByOrderId(orderId);
        assertThat(orders.stream().anyMatch(o ->
            o.getFulfillmentType() == FulfillmentType.VIRTUAL && o.getStatus().name().equals("ACTIVATED")
        )).isTrue();
    }

    @并且("应发布 ServiceActivated 事件且 orderId 为 {long}")
    public void 应发布ServiceActivated事件(long orderId) {
        var events = eventCapture.getServiceActivatedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    @并且("两次返回的 fulfillmentOrderIds 应一致")
    public void 两次返回的fulfillmentOrderIds应一致() {
        assertThat(context.getFirstFulfillmentOrderIds()).isNotNull();
        List<Long> currentIds = extractFulfillmentOrderIds(context.getLastResponseBody());
        assertThat(currentIds).isEqualTo(context.getFirstFulfillmentOrderIds());
    }

    private void postCreate(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "/api/fulfillment/create",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {}
            );
            context.setLastStatusCode(res.getStatusCode().value());
            context.setLastResponseBody(res.getBody());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
            context.setLastResponseBody(null);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractFulfillmentOrderIds(Map<String, Object> body) {
        if (body == null) return List.of();
        List<Number> ids = (List<Number>) body.get("fulfillmentOrderIds");
        if (ids == null) return List.of();
        return ids.stream().map(Number::longValue).toList();
    }
}
