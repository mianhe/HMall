package com.hmall.catalog.acceptance;

import java.util.List;

/**
 * 验收测试用 DTO，与 catalog-api.yaml 中 Product / ProductCreate / Error 一致。
 */
public final class ProductApiDto {

    /** 创建商品请求体 */
    public static class Create {
        public Long categoryId;
        public String name;
        public String description;
        public String productType;
        public String serviceKind;
    }

    /** 修改商品请求体 */
    public static class Update {
        public String name;
        public String description;
        public Long categoryId;
    }

    /** 商品响应体 */
    public static class Response {
        public Long id;
        public Long categoryId;
        public String name;
        public String description;
        public String productType;
        public String serviceKind;
        public String coverImageUrl;
        public List<ProductImageItem> defaultDisplayImages;
    }

    /** 默认展示图项（与 ProductImage 契约一致，测试仅反序列化） */
    public static class ProductImageItem {
        public Long id;
        public Long spuId;
        public Long specOptionId;
        public String imageUrl;
        public Integer sortOrder;
    }

    /** 错误响应体（400/404/422） */
    public static class Error {
        public String message;
    }

    private ProductApiDto() {}
}
