package com.hmall.catalog.acceptance;

import java.util.List;

/**
 * 验收测试用 DTO，与 api.yaml 中 ServiceBinding / AvailableService 一致。
 */
public final class ServiceBindingApiDto {

    public static class Create {
        public Long targetSpuId;
        public Long priceCents;
    }

    public static class Response {
        public Long id;
        public Long serviceSkuId;
        public Long targetSpuId;
        public Long priceCents;
        public String targetSpuName;
    }

    public static class AvailableServiceSku {
        public Long bindingId;
        public Long serviceSkuId;
        public Long priceCents;
        public List<SkuApiDto.SpecValue> specValues;
    }

    public static class AvailableService {
        public Long serviceSpuId;
        public String name;
        public String description;
        public String productType;
        public List<AvailableServiceSku> bindings;
    }

    public static class Error {
        public String message;
    }

    private ServiceBindingApiDto() {}
}
