package com.hmall.activity.acceptance;

import com.hmall.activity.application.RecordActivityCommand;
import com.hmall.activity.application.ActivityApplicationService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class ActivityStatsStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final ActivityApplicationService applicationService;

    private ResponseEntity<Map<String, Object>> lastStatsResponse;

    public ActivityStatsStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService) {
        this.restTemplate = restTemplate;
        this.applicationService = applicationService;
    }

    @假如("今日存在以下活动记录:")
    public void 今日存在以下活动记录(DataTable dataTable) {
        Instant now = Instant.now();
        for (Map<String, String> row : dataTable.asMaps()) {
            applicationService.record(new RecordActivityCommand(
                row.get("eventId"),
                row.get("eventType"),
                row.get("topic"),
                Long.parseLong(row.get("orderId")),
                "{}",
                now
            ));
        }
    }

    @假如("昨日存在以下活动记录:")
    public void 昨日存在以下活动记录(DataTable dataTable) {
        Instant yesterday = LocalDate.now().minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusHours(12)
            .toInstant();
        for (Map<String, String> row : dataTable.asMaps()) {
            applicationService.record(new RecordActivityCommand(
                row.get("eventId"),
                row.get("eventType"),
                row.get("topic"),
                Long.parseLong(row.get("orderId")),
                "{}",
                yesterday
            ));
        }
    }

    @当("查询统计不传参数")
    public void 查询统计不传参数() {
        lastStatsResponse = restTemplate.exchange(
            "/api/activities/stats",
            HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    }

    @当("查询统计 from {string} to {string}")
    public void 查询统计fromTo(String from, String to) {
        lastStatsResponse = restTemplate.exchange(
            "/api/activities/stats?from=" + from + "&to=" + to,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    }

    @当("查询统计 period {string}")
    public void 查询统计period(String period) {
        lastStatsResponse = restTemplate.exchange(
            "/api/activities/stats?period=" + period,
            HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {});
    }

    @那么("统计结果应为:")
    public void 统计结果应为(DataTable dataTable) {
        Map<String, Object> body = lastStatsResponse.getBody();
        if (body == null) {
            throw new AssertionError("统计结果为 null");
        }
        Map<String, String> expected = dataTable.asMaps().get(0);
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            int expectedVal = Integer.parseInt(entry.getValue());
            Number actualVal = (Number) body.get(entry.getKey());
            if (actualVal == null || actualVal.intValue() != expectedVal) {
                throw new AssertionError("期望 " + entry.getKey() + "=" + expectedVal
                    + "，实际 " + actualVal);
            }
        }
    }

    @那么("统计结果中 {word} 应为 {int}")
    public void 统计结果中某字段应为(String field, int expected) {
        Map<String, Object> body = lastStatsResponse.getBody();
        if (body == null) {
            throw new AssertionError("统计结果为 null");
        }
        Number actual = (Number) body.get(field);
        if (actual == null || actual.intValue() != expected) {
            throw new AssertionError("期望 " + field + "=" + expected + "，实际 " + actual);
        }
    }

    @并且("统计结果应回显 from {string} to {string}")
    public void 统计结果应回显fromTo(String expectedFrom, String expectedTo) {
        Map<String, Object> body = lastStatsResponse.getBody();
        if (body == null) {
            throw new AssertionError("统计结果为 null");
        }
        String actualFrom = (String) body.get("from");
        String actualTo = (String) body.get("to");
        if (!expectedFrom.equals(actualFrom)) {
            throw new AssertionError("期望 from=" + expectedFrom + "，实际 " + actualFrom);
        }
        if (!expectedTo.equals(actualTo)) {
            throw new AssertionError("期望 to=" + expectedTo + "，实际 " + actualTo);
        }
    }
}
