package com.hmall.activity.acceptance;

import com.hmall.activity.application.RecordActivityCommand;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.domain.ActivityRepository;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ActivityConsumeStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final ActivityApplicationService applicationService;
    private final ActivityRepository repository;

    private Long lastQueriedOrderId;

    public ActivityConsumeStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService,
            ActivityRepository repository) {
        this.restTemplate = restTemplate;
        this.applicationService = applicationService;
        this.repository = repository;
    }

    @假如("当前无订单 {long} 的活动记录")
    public void 当前无订单的活动记录(long orderId) {
        repository.deleteByOrderId(orderId);
    }

    @当("收到事件 eventId {string} eventType {string} topic {string} orderId {long} occurredAt {string}")
    public void 收到事件(String eventId, String eventType, String topic, long orderId, String occurredAtStr) {
        Instant occurredAt = Instant.parse(occurredAtStr);
        applicationService.record(new RecordActivityCommand(eventId, eventType, topic, orderId, null, null, "{}", occurredAt));
    }

    @那么("应存在一条活动记录 eventId {string} eventType {string} topic {string} orderId {long}")
    public void 应存在一条活动记录(String eventId, String eventType, String topic, long orderId) {
        lastQueriedOrderId = orderId;
        List<Map<String, Object>> list = getActivitiesByOrderId(orderId);
        if (list.size() != 1) {
            throw new AssertionError("期望 1 条活动记录，实际 " + list.size());
        }
        Map<String, Object> act = list.get(0);
        if (!eventId.equals(act.get("eventId"))) {
            throw new AssertionError("期望 eventId=" + eventId + "，实际 " + act.get("eventId"));
        }
        if (!eventType.equals(act.get("eventType"))) {
            throw new AssertionError("期望 eventType=" + eventType + "，实际 " + act.get("eventType"));
        }
        if (!topic.equals(act.get("topic"))) {
            throw new AssertionError("期望 topic=" + topic + "，实际 " + act.get("topic"));
        }
        Number oid = (Number) act.get("orderId");
        if (oid == null || oid.longValue() != orderId) {
            throw new AssertionError("期望 orderId=" + orderId + "，实际 " + act.get("orderId"));
        }
    }

    @并且("该记录的 occurredAt 为 {string}")
    public void 该记录的occurredAt为(String expectedOccurredAt) {
        if (lastQueriedOrderId == null) {
            throw new IllegalStateException("请先执行 那么 应存在一条活动记录");
        }
        List<Map<String, Object>> list = getActivitiesByOrderId(lastQueriedOrderId);
        if (list.isEmpty()) {
            throw new AssertionError("无活动记录可断言 occurredAt");
        }
        String actual = (String) list.get(0).get("occurredAt");
        if (actual == null || !Instant.parse(actual).equals(Instant.parse(expectedOccurredAt))) {
            throw new AssertionError("期望 occurredAt=" + expectedOccurredAt + "，实际 " + actual);
        }
    }

    @假如("已存在活动记录 eventId {string} eventType {string} topic {string} orderId {long} occurredAt {string}")
    public void 已存在活动记录(String eventId, String eventType, String topic, long orderId, String occurredAtStr) {
        repository.deleteByOrderId(orderId);
        Instant occurredAt = Instant.parse(occurredAtStr);
        applicationService.record(new RecordActivityCommand(eventId, eventType, topic, orderId, null, null, "{}", occurredAt));
    }

    @当("再次收到相同事件 eventId {string} eventType {string} topic {string} orderId {long} occurredAt {string}")
    public void 再次收到相同事件(String eventId, String eventType, String topic, long orderId, String occurredAtStr) {
        Instant occurredAt = Instant.parse(occurredAtStr);
        applicationService.record(new RecordActivityCommand(eventId, eventType, topic, orderId, null, null, "{}", occurredAt));
    }

    @那么("订单 {long} 的活动记录数应为 {int}")
    public void 活动记录数应为(long orderId, int expectedCount) {
        List<Map<String, Object>> list = getActivitiesByOrderId(orderId);
        if (list.size() != expectedCount) {
            throw new AssertionError("期望订单 " + orderId + " 有 " + expectedCount + " 条记录，实际 " + list.size());
        }
    }

    private List<Map<String, Object>> getActivitiesByOrderId(long orderId) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            "/api/activities?orderId=" + orderId + "&limit=50",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AssertionError("GET /api/activities?orderId=" + orderId + " 失败: " + response.getStatusCode());
        }
        return response.getBody();
    }
}
