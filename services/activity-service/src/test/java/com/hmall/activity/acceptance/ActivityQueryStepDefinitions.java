package com.hmall.activity.acceptance;

import com.hmall.activity.application.RecordActivityCommand;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.domain.ActivityRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ActivityQueryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final ActivityApplicationService applicationService;
    private final ActivityRepository repository;

    private ResponseEntity<List<Map<String, Object>>> lastListResponse;

    public ActivityQueryStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService,
            ActivityRepository repository) {
        this.restTemplate = restTemplate;
        this.applicationService = applicationService;
        this.repository = repository;
    }

    @假如("清空所有活动记录")
    public void 清空所有活动记录() {
        repository.deleteAll();
    }

    @假如("订单 {long} 有以下活动记录:")
    public void 订单有以下活动记录(long orderId, DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            applicationService.record(new RecordActivityCommand(
                row.get("eventId"),
                row.get("eventType"),
                row.get("topic"),
                orderId,
                null,
                null,
                "{}",
                Instant.parse(row.get("occurredAt"))
            ));
        }
    }

    @假如("存在以下活动记录:")
    public void 存在以下活动记录(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String payload = row.get("payload");
            if (payload == null || payload.isBlank()) payload = "{}";
            applicationService.record(new RecordActivityCommand(
                row.get("eventId"),
                row.get("eventType"),
                row.get("topic"),
                Long.parseLong(row.get("orderId")),
                null,
                null,
                payload,
                Instant.parse(row.get("occurredAt"))
            ));
        }
    }

    @当("按 orderId {long} 查询活动")
    public void 按orderId查询活动(long orderId) {
        lastListResponse = restTemplate.exchange(
            "/api/activities?orderId=" + orderId,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    }

    @当("查询最近活动 limit {int}")
    public void 查询最近活动(int limit) {
        lastListResponse = restTemplate.exchange(
            "/api/activities/recent?limit=" + limit,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    }

    @那么("应依次返回以下活动:")
    public void 应依次返回以下活动(DataTable dataTable) {
        List<Map<String, Object>> body = lastListResponse.getBody();
        List<Map<String, String>> expected = dataTable.asMaps();
        if (body == null || body.size() != expected.size()) {
            throw new AssertionError("期望 " + expected.size() + " 条记录，实际 "
                + (body == null ? 0 : body.size()));
        }
        for (int i = 0; i < expected.size(); i++) {
            String expectedType = expected.get(i).get("eventType");
            String actualType = (String) body.get(i).get("eventType");
            if (!expectedType.equals(actualType)) {
                throw new AssertionError("第 " + (i + 1) + " 条期望 eventType="
                    + expectedType + "，实际 " + actualType);
            }
        }
    }

    @那么("应返回空列表")
    public void 应返回空列表() {
        List<Map<String, Object>> body = lastListResponse.getBody();
        if (body == null || !body.isEmpty()) {
            throw new AssertionError("期望空列表，实际 " + (body == null ? "null" : body.size() + " 条"));
        }
    }
}
