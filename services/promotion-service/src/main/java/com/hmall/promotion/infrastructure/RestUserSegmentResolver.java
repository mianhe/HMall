package com.hmall.promotion.infrastructure;

import com.hmall.promotion.application.UserSegmentResolver;
import com.hmall.promotion.application.UserSegments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "user.base-url")
public class RestUserSegmentResolver implements UserSegmentResolver {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String userBaseUrl;

    public RestUserSegmentResolver(@Value("${user.base-url}") String userBaseUrl) {
        this.userBaseUrl = userBaseUrl;
    }

    @Override
    public UserSegments resolve(Long userId) {
        if (userId == null) {
            return new UserSegments("L1", Set.of());
        }
        try {
            String url = userBaseUrl + "/api/users/" + userId + "/segments";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return new UserSegments("L1", Set.of());
            }
            String level = body.get("level") == null ? "L1" : body.get("level").toString();
            @SuppressWarnings("unchecked")
            Set<String> tags = body.get("tags") instanceof java.util.List<?> list
                    ? list.stream().map(String::valueOf).collect(Collectors.toSet())
                    : Set.of();
            return new UserSegments(level, tags);
        } catch (RestClientException ex) {
            return new UserSegments("L1", Set.of());
        }
    }
}
