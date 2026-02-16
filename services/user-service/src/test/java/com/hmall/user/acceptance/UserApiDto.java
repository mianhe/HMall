package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public final class UserApiDto {

    public static class Create {
        public String username;
        public String password;
    }

    public static class Response {
        public Long id;
        public String username;
        public String passwordHash;
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

    private static final org.springframework.core.ParameterizedTypeReference<List<Response>> LIST_OF_USER =
        new org.springframework.core.ParameterizedTypeReference<>() {};

    public static ResponseEntity<List<Response>> getUsers(TestRestTemplate restTemplate) {
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
