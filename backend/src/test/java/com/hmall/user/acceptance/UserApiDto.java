package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/**
 * 验收测试用 DTO 与工具方法，与 user api.yaml 中 User / UserCreate 一致。
 */
public final class UserApiDto {

    /** 创建请求体 */
    public static class Create {
        public String username;
        public String password;
    }

    /** 响应体 */
    public static class Response {
        public Long id;
        public String username;
        public String passwordHash;  // 验收时不应出现，若出现则断言失败
    }

    public static ResponseEntity<Response> postUser(TestRestTemplate restTemplate, Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<Response> getUserById(TestRestTemplate restTemplate, long id) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + id,
                HttpMethod.GET,
                null,
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private static final org.springframework.core.ParameterizedTypeReference<java.util.List<Response>> LIST_OF_USER =
        new org.springframework.core.ParameterizedTypeReference<>() {};

    public static ResponseEntity<java.util.List<Response>> getUsers(TestRestTemplate restTemplate) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users",
                HttpMethod.GET,
                null,
                LIST_OF_USER
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private UserApiDto() {}
}
