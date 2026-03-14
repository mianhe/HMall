package com.hmall.activity.acceptance;

import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.application.RecordActivityCommand;
import com.hmall.activity.domain.ActivityRepository;
import com.hmall.activity.domain.OrderFactRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderFactStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final ActivityApplicationService applicationService;
    private final ActivityRepository activityRepository;
    private final OrderFactRepository orderFactRepository;

    private Long currentOrderId;
    private Instant nextOccurredAt = Instant.parse("2025-03-01T10:00:00Z");
    private ResponseEntity<Map> lastStatsResponse;
    private ResponseEntity<List<Map>> lastDailyStatsResponse;
    private ResponseEntity<Map> lastRankingResponse;
    private ResponseEntity<List<Map>> lastListResponse;

    public OrderFactStepDefinitions(TestRestTemplate restTemplate,
                                    ActivityApplicationService applicationService,
                                    ActivityRepository activityRepository,
                                    OrderFactRepository orderFactRepository) {
        this.restTemplate = restTemplate;
        this.applicationService = applicationService;
        this.activityRepository = activityRepository;
        this.orderFactRepository = orderFactRepository;
    }

    @假如("清空订单 {long} 的活动和事实记录")
    public void 清空订单的活动和事实记录(long orderId) {
        activityRepository.deleteByOrderId(orderId);
        orderFactRepository.deleteByOrderId(orderId);
        nextOccurredAt = Instant.parse("2025-03-01T10:00:00Z");
    }

    @假如("清空全部活动和事实记录")
    public void 清空全部活动和事实记录() {
        orderFactRepository.deleteAll();
        activityRepository.deleteAll();
        nextOccurredAt = Instant.parse("2025-03-01T10:00:00Z");
    }

    @并且("清空全部事实记录")
    public void 清空全部事实记录() {
        orderFactRepository.deleteAll();
    }

    @当("记录 OrderCreated 事件 orderId {long} userId {long} totalAmountCents {long} items:")
    public void 记录OrderCreated事件(long orderId, long userId, long totalAmountCents, DataTable itemsTable) throws Exception {
        currentOrderId = orderId;

        List<Map<String, String>> rows = itemsTable.asMaps();
        StringBuilder itemsJson = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            if (i > 0) itemsJson.append(",");
            itemsJson.append(String.format(
                "{\"skuId\":%s,\"spuId\":%s,\"quantity\":%s,\"unitPriceCents\":%s}",
                row.get("skuId"), row.get("spuId"), row.get("quantity"), row.get("unitPriceCents")));
        }
        itemsJson.append("]");

        String payload = String.format(
            "{\"orderId\":%d,\"userId\":%d,\"totalAmountCents\":%d,\"items\":%s}",
            orderId, userId, totalAmountCents, itemsJson);

        Instant occurredAt = advanceTime();
        applicationService.record(new RecordActivityCommand(
            UUID.randomUUID().toString(), "OrderCreated", "order.created",
            orderId, userId, null, payload, occurredAt));
    }

    @当("记录事件 eventType {string} orderId {long} payload {string}")
    public void 记录事件(String eventType, long orderId, String payload) {
        currentOrderId = orderId;
        String topic = topicFor(eventType);
        Instant occurredAt = advanceTime();
        applicationService.record(new RecordActivityCommand(
            UUID.randomUUID().toString(), eventType, topic,
            orderId, null, null, payload, occurredAt));
    }

    @那么("订单 {long} 的 OrderFact 应存在")
    public void 订单的OrderFact应存在(long orderId) {
        currentOrderId = orderId;
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/order-facts/" + orderId, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @那么("OrderFact 字段应为:")
    public void OrderFact字段应为(DataTable table) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/order-facts/" + currentOrderId, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();

        for (Map<String, String> row : table.asMaps()) {
            String field = row.get("field");
            String expected = row.get("value");
            Object actual = body.get(field);
            assertThat(String.valueOf(actual))
                .as("OrderFact.%s", field)
                .isEqualTo(expected);
        }
    }

    @并且("订单 {long} 应有 {int} 条 OrderItemFact")
    public void 订单应有N条OrderItemFact(long orderId, int count) {
        ResponseEntity<List> response = restTemplate.getForEntity(
            "/api/order-facts/" + orderId + "/items", List.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(count);
    }

    @并且("OrderFact 的 {word} 不为空")
    public void OrderFact的字段不为空(String field) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/order-facts/" + currentOrderId, Map.class);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get(field))
            .as("OrderFact.%s should not be null", field)
            .isNotNull();
    }

    @当("查询 stats from {string} to {string}")
    public void 查询stats(String from, String to) {
        lastStatsResponse = restTemplate.getForEntity(
            "/api/order-facts/stats?from=" + from + "&to=" + to, Map.class);
        assertThat(lastStatsResponse.getStatusCode().value()).isEqualTo(200);
    }

    @那么("stats 响应字段应为:")
    public void stats响应字段应为(DataTable table) {
        Map<String, Object> body = lastStatsResponse.getBody();
        assertThat(body).isNotNull();
        for (Map<String, String> row : table.asMaps()) {
            String field = row.get("field");
            String expected = row.get("value");
            assertThat(String.valueOf(body.get(field)))
                .as("stats.%s", field)
                .isEqualTo(expected);
        }
    }

    @当("查询 stats_daily from {string} to {string}")
    public void 查询statsDailyRaw(String from, String to) {
        ResponseEntity<List> raw = restTemplate.getForEntity(
            "/api/order-facts/stats/daily?from=" + from + "&to=" + to, List.class);
        assertThat(raw.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map> body = (List<Map>) raw.getBody();
        lastDailyStatsResponse = ResponseEntity.ok(body);
    }

    @那么("stats_daily 应有 {int} 天记录")
    public void statsDailyCount(int count) {
        assertThat(lastDailyStatsResponse.getBody()).hasSize(count);
    }

    @并且("第 {int} 天的 totalOrders 为 {int}")
    public void 第N天totalOrders(int n, int expected) {
        Map day = lastDailyStatsResponse.getBody().get(n - 1);
        assertThat(((Number) day.get("totalOrders")).intValue()).isEqualTo(expected);
    }

    @当("查询 product_ranking rankBy {string} from {string} to {string}")
    public void 查询productRanking(String rankBy, String from, String to) {
        lastRankingResponse = restTemplate.getForEntity(
            "/api/order-facts/product-ranking?rankBy=" + rankBy + "&from=" + from + "&to=" + to, Map.class);
        assertThat(lastRankingResponse.getStatusCode().value()).isEqualTo(200);
    }

    @那么("product_ranking 排名第 {int} 的 spuId 为 {long}")
    public void rankingSpuId(int rank, long expectedSpuId) {
        @SuppressWarnings("unchecked")
        List<Map> items = (List<Map>) lastRankingResponse.getBody().get("items");
        assertThat(((Number) items.get(rank - 1).get("spuId")).longValue()).isEqualTo(expectedSpuId);
    }

    @并且("product_ranking 排名第 {int} 的 totalQuantity 为 {int}")
    public void rankingTotalQuantity(int rank, int expected) {
        @SuppressWarnings("unchecked")
        List<Map> items = (List<Map>) lastRankingResponse.getBody().get("items");
        assertThat(((Number) items.get(rank - 1).get("totalQuantity")).intValue()).isEqualTo(expected);
    }

    @当("查询 order-facts hasEngraving {word} from {string} to {string}")
    public void 查询orderFacts(String hasEng, String from, String to) {
        ResponseEntity<List> raw = restTemplate.getForEntity(
            "/api/order-facts?hasEngraving=" + hasEng + "&from=" + from + "&to=" + to, List.class);
        assertThat(raw.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map> body = (List<Map>) raw.getBody();
        lastListResponse = ResponseEntity.ok(body);
    }

    @那么("列表应返回 {int} 条记录")
    public void 列表count(int count) {
        assertThat(lastListResponse.getBody()).hasSize(count);
    }

    @并且("列表第 {int} 条的 orderId 为 {long}")
    public void 列表orderId(int n, long expected) {
        Map item = lastListResponse.getBody().get(n - 1);
        assertThat(((Number) item.get("orderId")).longValue()).isEqualTo(expected);
    }

    @当("调用重建订单事实投影")
    public void 调用rebuild() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/order-facts/rebuild", null, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    private Instant advanceTime() {
        Instant t = nextOccurredAt;
        nextOccurredAt = nextOccurredAt.plusSeconds(300);
        return t;
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "OrderCreated" -> "order.created";
            case "OrderCancelled" -> "order.cancelled";
            case "OrderCompleted" -> "order.completed";
            case "PaymentCompleted" -> "payment.completed";
            case "PaymentFailed" -> "payment.failed";
            case "PaymentExpired" -> "payment.expired";
            case "StockReserved" -> "inventory.stock.reserved";
            case "StockReleased" -> "inventory.stock.released";
            case "FulfillmentOrderCreated" -> "fulfillment.order.created";
            case "FulfillmentOrderAllocated" -> "fulfillment.order.allocated";
            case "FulfillmentShipped" -> "fulfillment.shipped";
            case "FulfillmentDelivered" -> "fulfillment.delivered";
            case "EngravingCompleted" -> "fulfillment.engraving.completed";
            case "ServiceActivated" -> "fulfillment.service.activated";
            default -> eventType.toLowerCase();
        };
    }
}
