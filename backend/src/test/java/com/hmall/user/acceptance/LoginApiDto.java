package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/**
 * 验收测试用 DTO 与工具方法，与 user api.yaml 中 LoginRequest / LoginResponse 一致。
 */
public final class LoginApiDto {

    /** 登录请求体 */
    public static class Request {
        public String username;
        public String password;
    }

    /** 登录响应体 */
    public static class Response {
        public String token;
    }

    public static ResponseEntity<Response> postLogin(TestRestTemplate restTemplate, Request body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/login",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private LoginApiDto() {}
}
