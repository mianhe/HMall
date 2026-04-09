package com.hmall.user.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Set;

public final class UserApiDto {

    public static class Create {
        public String username;
        public String password;
    }

    public static class Response {
        public Long id;
        public String username;
        public String passwordHash;
        public String level;
        public Set<String> tags;
    }

    public static class SegmentsResponse {
        public Long userId;
        public String level;
        public Set<String> tags;
    }

    public static class UpdateLevelRequest {
        public String level;
    }

    public static class UpdateTagsRequest {
        public Set<String> tags;
    }

    public static class SegmentCondition {
        public Set<String> levelsIn;
        public Set<String> tagsAny;
        public Set<String> tagsAll;
        public Set<String> excludeTags;
    }

    public static class CreateSegmentRuleRequest {
        public String name;
        public SegmentCondition conditions;
    }

    public static class SegmentRuleResponse {
        public Long ruleId;
        public String name;
        public String status;
        public SegmentCondition conditions;
        public Long lastPreviewCount;
        public String updatedAt;
    }

    public static class SegmentRulePreviewRequest {
        public Integer sampleSize;
    }

    public static class ReasonStatResponse {
        public String reason;
        public Long count;
    }

    public static class SegmentRulePreviewResponse {
        public Long ruleId;
        public Long hitCount;
        public List<Long> sampleUserIds;
        public List<ReasonStatResponse> reasonStats;
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

    public static ResponseEntity<SegmentsResponse> getUserSegmentsById(TestRestTemplate restTemplate, long id) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + id + "/segments",
                HttpMethod.GET,
                null,
                SegmentsResponse.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<Response> updateUserLevel(TestRestTemplate restTemplate, long id, String level) {
        UpdateLevelRequest body = new UpdateLevelRequest();
        body.level = level;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + id + "/level",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<Response> updateUserTags(TestRestTemplate restTemplate, long id, Set<String> tags) {
        UpdateTagsRequest body = new UpdateTagsRequest();
        body.tags = tags;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/" + id + "/tags",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<SegmentRuleResponse> createSegmentRule(
        TestRestTemplate restTemplate,
        CreateSegmentRuleRequest body
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/segment-rules",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                SegmentRuleResponse.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<SegmentRulePreviewResponse> previewSegmentRule(
        TestRestTemplate restTemplate,
        long ruleId,
        Integer sampleSize
    ) {
        SegmentRulePreviewRequest body = new SegmentRulePreviewRequest();
        body.sampleSize = sampleSize;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/segment-rules/" + ruleId + "/preview",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                SegmentRulePreviewResponse.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    public static ResponseEntity<SegmentRuleResponse> activateSegmentRule(
        TestRestTemplate restTemplate,
        long ruleId
    ) {
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/users/segment-rules/" + ruleId + "/activate",
                HttpMethod.POST,
                null,
                SegmentRuleResponse.class
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
