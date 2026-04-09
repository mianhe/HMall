package com.hmall.user.acceptance;

import com.hmall.user.acceptance.config.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class UserStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final Map<String, Long> usernameToId = new ConcurrentHashMap<>();

    public Map<String, Long> getUsernameToId() {
        return usernameToId;
    }

    private ResponseEntity<UserApiDto.Response> lastUserResponse;
    private ResponseEntity<List<UserApiDto.Response>> lastListResponse;
    private ResponseEntity<UserApiDto.SegmentsResponse> lastSegmentsResponse;
    private ResponseEntity<UserApiDto.SegmentRuleResponse> lastSegmentRuleResponse;
    private ResponseEntity<UserApiDto.SegmentRulePreviewResponse> lastSegmentPreviewResponse;
    private Long lastSegmentRuleId;

    public UserStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
    }

    private void setLastStatusCode(int statusCode) {
        lastResponseContext.setLastStatusCode(statusCode);
    }

    @When("用户创建用户 {string} 密码 {string}")
    public void 用户创建用户(String username, String password) {
        UserApiDto.Create body = new UserApiDto.Create();
        body.username = username;
        body.password = password;
        lastUserResponse = UserApiDto.postUser(restTemplate, body);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
        if (lastUserResponse.getStatusCode().is2xxSuccessful() && lastUserResponse.getBody() != null) {
            usernameToId.put(username, lastUserResponse.getBody().id);
        }
    }

    @And("返回的 userId 不为空")
    public void 返回的userId不为空() {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().id).isNotNull();
    }

    @And("返回的 username 为 {string}")
    public void 返回的username为(String expected) {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().username).isEqualTo(expected);
    }

    @Given("已存在用户 {string} 密码 {string}")
    public void 已存在用户(String username, String password) {
        UserApiDto.Create body = new UserApiDto.Create();
        body.username = username;
        body.password = password;
        ResponseEntity<UserApiDto.Response> res = UserApiDto.postUser(restTemplate, body);
        setLastStatusCode(res.getStatusCode().value());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            usernameToId.put(username, res.getBody().id);
        }
    }

    @Given("已存在用户 {string} 和 {string}")
    public void 已存在用户和(String username1, String username2) {
        已存在用户(username1, "pass1");
        已存在用户(username2, "pass2");
    }

    @When("用户请求用户 {string} 的详情")
    public void 用户请求用户详情(String username) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        lastUserResponse = UserApiDto.getUserById(restTemplate, id);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @When("用户请求用户 ID {long} 的详情")
    public void 用户请求用户ID详情(long id) {
        lastUserResponse = UserApiDto.getUserById(restTemplate, id);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @When("用户请求用户 {string} 的分群信息")
    public void 用户请求用户分群信息(String username) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        lastSegmentsResponse = UserApiDto.getUserSegmentsById(restTemplate, id);
        setLastStatusCode(lastSegmentsResponse.getStatusCode().value());
    }

    @When("运营将用户 {string} 的等级更新为 {string}")
    public void 运营更新用户等级(String username, String level) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        lastUserResponse = UserApiDto.updateUserLevel(restTemplate, id, level);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @When("运营将用户 {string} 的标签更新为 {string}")
    public void 运营更新用户标签(String username, String tagsCsv) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        Set<String> tags = parseTags(tagsCsv);
        lastUserResponse = UserApiDto.updateUserTags(restTemplate, id, tags);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @When("运营创建圈选规则 {string} 条件 levelsIn {string} tagsAny {string} tagsAll {string} excludeTags {string}")
    public void 运营创建圈选规则(
        String ruleName,
        String levelsInCsv,
        String tagsAnyCsv,
        String tagsAllCsv,
        String excludeTagsCsv
    ) {
        UserApiDto.CreateSegmentRuleRequest request = new UserApiDto.CreateSegmentRuleRequest();
        request.name = ruleName;
        request.conditions = new UserApiDto.SegmentCondition();
        request.conditions.levelsIn = parseTags(levelsInCsv);
        request.conditions.tagsAny = parseTags(tagsAnyCsv);
        request.conditions.tagsAll = parseTags(tagsAllCsv);
        request.conditions.excludeTags = parseTags(excludeTagsCsv);
        lastSegmentRuleResponse = UserApiDto.createSegmentRule(restTemplate, request);
        setLastStatusCode(lastSegmentRuleResponse.getStatusCode().value());
        if (lastSegmentRuleResponse.getBody() != null) {
            lastSegmentRuleId = lastSegmentRuleResponse.getBody().ruleId;
        }
    }

    @When("运营预览最近创建的圈选规则")
    public void 运营预览最近创建的圈选规则() {
        assertThat(lastSegmentRuleId).isNotNull();
        lastSegmentPreviewResponse = UserApiDto.previewSegmentRule(restTemplate, lastSegmentRuleId, 20);
        setLastStatusCode(lastSegmentPreviewResponse.getStatusCode().value());
    }

    @When("运营激活最近创建的圈选规则")
    public void 运营激活最近创建的圈选规则() {
        assertThat(lastSegmentRuleId).isNotNull();
        lastSegmentRuleResponse = UserApiDto.activateSegmentRule(restTemplate, lastSegmentRuleId);
        setLastStatusCode(lastSegmentRuleResponse.getStatusCode().value());
    }

    @And("返回的用户信息包含 username {string}")
    public void 返回的用户信息包含username(String expected) {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().username).isEqualTo(expected);
    }

    @And("返回的用户信息不含 passwordHash")
    public void 返回的用户信息不含passwordHash() {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().passwordHash).isNull();
    }

    @When("用户请求用户列表")
    public void 用户请求用户列表() {
        lastListResponse = UserApiDto.getUsers(restTemplate);
        setLastStatusCode(lastListResponse.getStatusCode().value());
    }

    @And("返回的用户列表中至少有 {int} 个用户")
    public void 返回的用户列表中至少有(int minCount) {
        assertThat(lastListResponse).isNotNull();
        assertThat(lastListResponse.getBody()).hasSizeGreaterThanOrEqualTo(minCount);
    }

    @And("返回的列表中包含 username {string} 和 {string}")
    public void 返回的列表中包含username和(String name1, String name2) {
        List<String> usernames = lastListResponse.getBody().stream()
            .map(u -> u.username)
            .toList();
        assertThat(usernames).contains(name1, name2);
    }

    @And("返回的用户分群 level 为 {string}")
    public void 返回的用户分群level为(String expectedLevel) {
        assertThat(lastSegmentsResponse.getBody()).isNotNull();
        assertThat(lastSegmentsResponse.getBody().level).isEqualTo(expectedLevel);
    }

    @And("返回的用户分群 tags 为空")
    public void 返回的用户分群tags为空() {
        assertThat(lastSegmentsResponse.getBody()).isNotNull();
        assertThat(lastSegmentsResponse.getBody().tags).isEmpty();
    }

    @And("返回的用户分群 tags 包含 {string}")
    public void 返回的用户分群tags包含(String expectedTag) {
        assertThat(lastSegmentsResponse.getBody()).isNotNull();
        assertThat(lastSegmentsResponse.getBody().tags).contains(expectedTag);
    }

    @And("圈选规则状态为 {string}")
    public void 圈选规则状态为(String expectedStatus) {
        assertThat(lastSegmentRuleResponse.getBody()).isNotNull();
        assertThat(lastSegmentRuleResponse.getBody().status).isEqualTo(expectedStatus);
    }

    @And("圈选预览命中人数为 {long}")
    public void 圈选预览命中人数为(long expectedHitCount) {
        assertThat(lastSegmentPreviewResponse.getBody()).isNotNull();
        assertThat(lastSegmentPreviewResponse.getBody().hitCount).isEqualTo(expectedHitCount);
    }

    private Set<String> parseTags(String csv) {
        if (csv == null || csv.isBlank() || "-".equals(csv.trim())) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toSet());
    }
}
