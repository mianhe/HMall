package com.hmall.smartinteraction.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SkillManagementStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final SkillTestContext ctx;

    @LocalServerPort
    private int port;

    public SkillManagementStepDefinitions(TestRestTemplate restTemplate, SkillTestContext ctx) {
        this.restTemplate = restTemplate;
        this.ctx = ctx;
    }

    @Before("@skill")
    public void cleanup() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = restTemplate.getForObject("/api/ai/skills", List.class);
        if (skills != null) {
            for (Map<String, Object> s : skills) {
                Number id = (Number) s.get("id");
                restTemplate.delete("/api/ai/skills/" + id.longValue());
            }
        }
    }

    // ---- 创建 ----

    @当("创建 Skill 名称 {string} 描述 {string} systemPrompt {string} allowedTools {string}")
    public void 创建Skill(String name, String description, String systemPrompt, String allowedToolsStr) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("systemPrompt", systemPrompt);
        if (!allowedToolsStr.isEmpty()) {
            body.put("allowedTools", Arrays.asList(allowedToolsStr.split(",")));
        } else {
            body.put("allowedTools", List.of());
        }
        post("/api/ai/skills", body);
    }

    @那么("返回的 Skill 名称为 {string}")
    public void 返回的Skill名称为(String expected) {
        assertThat(ctx.getLastResponseBody().get("name")).isEqualTo(expected);
    }

    @并且("返回的 Skill 包含 allowedTools {string} 和 {string}")
    public void 返回的Skill包含allowedTools(String tool1, String tool2) {
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) ctx.getLastResponseBody().get("allowedTools");
        assertThat(tools).contains(tool1, tool2);
    }

    @并且("返回的 Skill 包含 allowedTools {string}")
    public void 返回的Skill包含allowedTool(String tool) {
        @SuppressWarnings("unchecked")
        List<String> tools = (List<String>) ctx.getLastResponseBody().get("allowedTools");
        assertThat(tools).contains(tool);
    }

    // ---- Given: 已存在 Skill ----

    @假如("已存在 Skill {string}")
    public void 已存在Skill(String name) {
        createSkillHelper(name, "", "", List.of(), false);
    }

    @假如("已存在 Skill {string} 且 allowedTools 为 {string}")
    public void 已存在Skill且allowedTools(String name, String toolsStr) {
        createSkillHelper(name, "", "", Arrays.asList(toolsStr.split(",")), false);
    }

    @假如("已存在默认 Skill {string}")
    public void 已存在默认Skill(String name) {
        createSkillHelper(name, "", "", List.of(), true);
    }

    // ---- 列表 ----

    @当("查询 Skill 列表")
    public void 查询Skill列表() {
        get("/api/ai/skills");
    }

    @并且("列表中应包含 {string} 和 {string}")
    public void 列表中应包含(String name1, String name2) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) (Object) ctx.getLastResponseBody().get("_list");
        List<String> names = list.stream().map(s -> (String) s.get("name")).toList();
        assertThat(names).contains(name1, name2);
    }

    @那么("列表中无 isDefault 为 true 的 Skill")
    public void 列表中无默认Skill() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) (Object) ctx.getLastResponseBody().get("_list");
        boolean hasDefault = list.stream().anyMatch(s -> Boolean.TRUE.equals(s.get("isDefault")));
        assertThat(hasDefault).isFalse();
    }

    // ---- 详情 ----

    @当("按 ID 查询该 Skill")
    public void 按ID查询该Skill() {
        get("/api/ai/skills/" + ctx.getLastSkillId());
    }

    @当("按 ID 查询 {string}")
    public void 按ID查询(String name) {
        get("/api/ai/skills/" + ctx.getSkillIdByName(name));
    }

    // ---- 更新 ----

    @当("更新该 Skill 名称为 {string} 描述为 {string}")
    public void 更新该Skill(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("systemPrompt", "");
        body.put("allowedTools", List.of());
        put("/api/ai/skills/" + ctx.getLastSkillId(), body);
    }

    // ---- 删除 ----

    @当("删除该 Skill")
    public void 删除该Skill() {
        delete("/api/ai/skills/" + ctx.getLastSkillId());
    }

    // ---- 默认设置 ----

    @当("将 {string} 设为默认")
    public void 将Skill设为默认(String name) {
        Long id = ctx.getSkillIdByName(name);
        put("/api/ai/skills/" + id + "/default", Map.of());
    }

    @并且("{string} 的 isDefault 为 true")
    public void 的isDefault为true(String name) {
        Long id = ctx.getSkillIdByName(name);
        get("/api/ai/skills/" + id);
        assertThat(ctx.getLastResponseBody().get("isDefault")).isEqualTo(true);
    }

    @那么("{string} 的 isDefault 为 false")
    public void 的isDefault为false(String name) {
        Long id = ctx.getSkillIdByName(name);
        get("/api/ai/skills/" + id);
        assertThat(ctx.getLastResponseBody().get("isDefault")).isEqualTo(false);
    }

    // ---- 通用断言 ----

    @那么("应返回 {int}")
    public void 应返回(int expected) {
        assertThat(ctx.getLastStatusCode()).isEqualTo(expected);
    }

    // ---- helpers ----

    private void createSkillHelper(String name, String desc, String prompt, List<String> tools, boolean setDefault) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", desc);
        body.put("systemPrompt", prompt);
        body.put("allowedTools", tools);
        post("/api/ai/skills", body);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = ctx.getLastResponseBody();
        Long id = ((Number) resp.get("id")).longValue();
        ctx.setLastSkillId(id);
        ctx.putSkillId(name, id);

        if (setDefault) {
            put("/api/ai/skills/" + id + "/default", Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    private void post(String url, Map<String, Object> body) {
        try {
            ResponseEntity<Map> res = restTemplate.postForEntity(url, body, Map.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
            ctx.setLastResponseBody(res.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ctx.setLastStatusCode(e.getStatusCode().value());
            ctx.setLastResponseBody(Map.of("error", e.getStatusText()));
        }
    }

    @SuppressWarnings("unchecked")
    private void get(String url) {
        try {
            ResponseEntity<Object> res = restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
            Object resBody = res.getBody();
            if (resBody instanceof List) {
                ctx.setLastResponseBody(Map.of("_list", resBody));
            } else if (resBody instanceof Map) {
                ctx.setLastResponseBody((Map<String, Object>) resBody);
            }
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

    private void delete(String url) {
        try {
            ResponseEntity<Void> res = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
            ctx.setLastStatusCode(res.getStatusCode().value());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ctx.setLastStatusCode(e.getStatusCode().value());
        }
    }
}
