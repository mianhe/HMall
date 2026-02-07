package com.hmall.catalog.acceptance;

/**
 * 验收测试用 DTO，与 catalog-api.yaml 中 Category / CategoryCreate 一致。
 */
public final class CategoryApiDto {

    /** 创建请求体 */
    public static class Create {
        public String name;
        public String description;
        public Long parentId;
    }

    /** 响应体 */
    public static class Response {
        public Long id;
        public Long parentId;
        public String name;
        public String description;
    }

    private CategoryApiDto() {}
}
