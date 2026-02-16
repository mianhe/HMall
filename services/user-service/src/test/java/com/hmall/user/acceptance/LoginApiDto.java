package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

public final class LoginApiDto {

    public static class Request {
        public String username;
        public String password;
    }

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
