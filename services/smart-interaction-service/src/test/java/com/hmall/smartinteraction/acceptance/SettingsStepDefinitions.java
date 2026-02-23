package com.hmall.smartinteraction.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final SkillTestContext ctx;

    public SettingsStepDefinitions(TestRestTemplate restTemplate, SkillTestContext ctx) {
        this.restTemplate = restTemplate;
        this.ctx = ctx;
    }

    @Before("@settings")
    public void resetSettings() {
        Map<String, Object> body = new HashMap<>();
        body.put("adminBasePrompt", "");
        body.put("consumerBasePrompt", "");
        put("/api/ai/settings", body);
    }

    @假如("系统设置已重置")
    public void 系统设置已重置() {
        // handled by @Before("@settings")
    }

    @假如("已更新系统设置 adminBasePrompt 为 {string}")
    public void 已更新AdminBasePrompt(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("adminBasePrompt", prompt);
        put("/api/ai/settings", body);
    }

    @当("获取系统设置")
    public void 获取系统设置() {
        get("/api/ai/settings");
    }

    @当("更新系统设置 adminBasePrompt 为 {string}")
    public void 更新AdminBasePrompt(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("adminBasePrompt", prompt);
        put("/api/ai/settings", body);
    }

    @当("更新系统设置 consumerBasePrompt 为 {string}")
    public void 更新ConsumerBasePrompt(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("consumerBasePrompt", prompt);
        put("/api/ai/settings", body);
    }

    @当("更新系统设置 adminBasePrompt 为 {string} consumerBasePrompt 为 {string}")
    public void 更新两端Prompt(String admin, String consumer) {
        Map<String, Object> body = new HashMap<>();
        body.put("adminBasePrompt", admin);
        body.put("consumerBasePrompt", consumer);
        put("/api/ai/settings", body);
    }

    @那么("返回的 adminBasePrompt 为 {string}")
    public void 返回的AdminBasePrompt为(String expected) {
        assertThat(ctx.getLastResponseBody().get("adminBasePrompt")).isEqualTo(expected);
    }

    @并且("返回的 consumerBasePrompt 为 {string}")
    public void 返回的ConsumerBasePrompt为(String expected) {
        assertThat(ctx.getLastResponseBody().get("consumerBasePrompt")).isEqualTo(expected);
    }

    @当("重置系统设置")
    public void 重置系统设置() {
        post("/api/ai/settings/reset");
    }

    @并且("adminBasePrompt 为空")
    public void adminBasePrompt为空() {
        assertThat(ctx.getLastResponseBody().get("adminBasePrompt")).isNull();
    }

    @并且("consumerBasePrompt 为空")
    public void consumerBasePrompt为空() {
        assertThat(ctx.getLastResponseBody().get("consumerBasePrompt")).isNull();
    }

    @并且("adminBasePrompt 不为空")
    public void adminBasePrompt不为空() {
        Object val = ctx.getLastResponseBody().get("adminBasePrompt");
        assertThat(val).isNotNull();
        assertThat(val.toString()).isNotBlank();
    }

    @并且("consumerBasePrompt 不为空")
    public void consumerBasePrompt不为空() {
        Object val = ctx.getLastResponseBody().get("consumerBasePrompt");
        assertThat(val).isNotNull();
        assertThat(val.toString()).isNotBlank();
    }

    @SuppressWarnings("unchecked")
    private void get(String url) {
        try {
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
            ctx.setLastResponseBody(res.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ctx.setLastStatusCode(e.getStatusCode().value());
            ctx.setLastResponseBody(Map.of("error", e.getStatusText()));
        }
    }

    @SuppressWarnings("unchecked")
    private void put(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
            ctx.setLastResponseBody(res.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ctx.setLastStatusCode(e.getStatusCode().value());
            ctx.setLastResponseBody(Map.of("error", e.getStatusText()));
        }
    }

    @SuppressWarnings("unchecked")
    private void post(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
            ctx.setLastResponseBody(res.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ctx.setLastStatusCode(e.getStatusCode().value());
            ctx.setLastResponseBody(Map.of("error", e.getStatusText()));
        }
    }
}
