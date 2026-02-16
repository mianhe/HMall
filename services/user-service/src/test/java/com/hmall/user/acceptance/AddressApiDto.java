package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public final class AddressApiDto {

    public static class Create {
        public String recipientName;
        public String phone;
        public String province;
        public String city;
        public String district;
        public String detail;
    }

    public static class Response {
        public Long addressId;
        public Long userId;
        public String recipientName;
        public String phone;
        public String province;
        public String city;
        public String district;
        public String detail;
    }

    public static ResponseEntity<Response> postAddress(TestRestTemplate restTemplate, long userId, Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + userId + "/addresses",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<List<Response>> getAddresses(TestRestTemplate restTemplate, long userId) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + userId + "/addresses",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Response>>() {}
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<Response> getAddress(TestRestTemplate restTemplate, long userId, long addressId) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + userId + "/addresses/" + addressId,
                HttpMethod.GET,
                null,
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static class Update {
        public String recipientName;
        public String phone;
        public String province;
        public String city;
        public String district;
        public String detail;
    }

    public static ResponseEntity<Response> putAddress(TestRestTemplate restTemplate, long userId, long addressId, Update body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + userId + "/addresses/" + addressId,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<Void> deleteAddress(TestRestTemplate restTemplate, long userId, long addressId) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + userId + "/addresses/" + addressId,
                HttpMethod.DELETE,
                null,
                Void.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    private AddressApiDto() {}
}
