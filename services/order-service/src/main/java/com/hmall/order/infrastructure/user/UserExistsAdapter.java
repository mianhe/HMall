package com.hmall.order.infrastructure.user;

import com.hmall.order.application.port.UserExistsPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 HTTP 调用 User 服务的 UserExistsPort 实现。
 */
@Component
public class UserExistsAdapter implements UserExistsPort {

    private final RestTemplate restTemplate;
    private final String userBaseUrl;

    public UserExistsAdapter(
            RestTemplate restTemplate,
            @Value("${user.base-url:http://localhost:8082}") String userBaseUrl) {
        this.restTemplate = restTemplate;
        this.userBaseUrl = userBaseUrl;
    }

    @Override
    public void requireExists(Long userId) {
        String url = userBaseUrl + "/api/users/" + userId;
        try {
            restTemplate.getForObject(url, Object.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("用户不存在");
            }
            throw e;
        }
    }
}
