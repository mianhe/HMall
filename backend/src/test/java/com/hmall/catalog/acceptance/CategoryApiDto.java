package com.hmall.catalog.acceptance;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/**
 * 验收测试用 DTO 与工具方法，与 catalog-api.yaml 中 Category / CategoryCreate / CategoryUpdate 一致。
 */
public final class CategoryApiDto {

    /** 创建请求体 */
    public static class Create {
        public String name;
        public String description;
        public Long parentId;
    }

    /** 修改请求体 */
    public static class Update {
        public String name;
        public String description;
    }

    /** 响应体 */
    public static class Response {
        public Long id;
        public Long parentId;
        public String name;
        public String description;
    }

    /** 通过 API 创建类别（供多个 StepDefinitions 复用） */
    public static ResponseEntity<Response> postCategory(TestRestTemplate restTemplate, Create body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.exchange(
                restTemplate.getRootUri() + "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Response.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    private CategoryApiDto() {}
}
