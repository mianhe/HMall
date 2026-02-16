package com.hmall.catalog.acceptance;

import java.util.List;

/**
 * 验收测试用 DTO，与 catalog-api.yaml 中 SpecDimension / SpecOption / OptionImage / SpecDimensionWithOptions 一致。
 */
public final class SpecDimensionApiDto {

    public static class DimensionCreate {
        public String name;
        public Boolean required;
        public Integer sortOrder;
    }

    public static class DimensionResponse {
        public Long id;
        public Long spuId;
        public String name;
        public Boolean required;
        public Integer sortOrder;
    }

    public static class OptionCreate {
        public String optionValue;
        public Integer sortOrder;
    }

    public static class OptionResponse {
        public Long id;
        public Long specDimensionId;
        public String optionValue;
        public Integer sortOrder;
        public List<OptionImageResponse> images;
    }

    public static class OptionImageCreate {
        public String imageUrl;
        public Integer sortOrder;
    }

    public static class OptionImageResponse {
        public Long id;
        public Long specOptionId;
        public String imageUrl;
        public Integer sortOrder;
    }

    public static class ProductImageCreate {
        public String imageUrl;
        public Integer sortOrder;
    }

    public static class ProductImageResponse {
        public Long id;
        public Long spuId;
        public Long specOptionId;
        public String imageUrl;
        public Integer sortOrder;
    }

    /** GET 维度及选项时：维度 + options 列表 */
    public static class DimensionWithOptions {
        public Long id;
        public Long spuId;
        public String name;
        public Boolean required;
        public Integer sortOrder;
        public List<OptionResponse> options;
    }

    public static class Error {
        public String message;
    }

    private SpecDimensionApiDto() {}
}
