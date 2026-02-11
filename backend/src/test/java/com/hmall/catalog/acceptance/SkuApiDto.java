package com.hmall.catalog.acceptance;

import java.util.List;

/**
 * 验收测试用 DTO，与 catalog-api.yaml 中 Sku / SkuCreate 一致。
 */
public final class SkuApiDto {

    public static class Create {
        public List<Long> specOptionIds;
        public Long priceCents;
        public String displayName;
    }

    public static class Update {
        public Long priceCents;
        public String displayName;
    }

    public static class SpecValue {
        public String dimensionName;
        public String optionValue;
    }

    public static class Response {
        public Long id;
        public Long spuId;
        public Long priceCents;
        public String displayName;
        public List<SpecValue> specValues;
    }

    public static class Error {
        public String message;
    }

    private SkuApiDto() {}
}
