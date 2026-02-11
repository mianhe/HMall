package com.hmall.catalog.acceptance;

/**
 * 验收测试用 DTO，与 catalog-api.yaml 中 Product / ProductCreate / Error 一致。
 */
public final class ProductApiDto {

    /** 创建商品请求体 */
    public static class Create {
        public Long categoryId;
        public String name;
        public String description;
    }

    /** 修改商品请求体 */
    public static class Update {
        public String name;
        public String description;
    }

    /** 商品响应体 */
    public static class Response {
        public Long id;
        public Long categoryId;
        public String name;
        public String description;
    }

    /** 错误响应体（400/404/422） */
    public static class Error {
        public String message;
    }

    private ProductApiDto() {}
}
