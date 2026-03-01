package com.hmall.order.acceptance;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

public class OrderApiDto {

    public static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static class CreateRequest {
        public Long userId;
        public List<LineItemCreate> items;
        public ShippingAddress shippingAddress;
    }

    public static class LineItemCreate {
        public Long skuId;
        public Integer quantity;
        public Long relatedSkuId;

        public LineItemCreate() {}

        public LineItemCreate(Long skuId, Integer quantity) {
            this.skuId = skuId;
            this.quantity = quantity;
        }

        public LineItemCreate(Long skuId, Integer quantity, Long relatedSkuId) {
            this.skuId = skuId;
            this.quantity = quantity;
            this.relatedSkuId = relatedSkuId;
        }
    }

    public static class ShippingAddress {
        public String recipientName;
        public String phone;
        public String province;
        public String city;
        public String district;
        public String detail;

        public ShippingAddress() {}

        public ShippingAddress(String recipientName, String phone, String province, String city, String district, String detail) {
            this.recipientName = recipientName;
            this.phone = phone;
            this.province = province;
            this.city = city;
            this.district = district;
            this.detail = detail;
        }
    }

    public static class CreateResponse {
        public Long orderId;
        public String status;
        public Long totalAmountCents;
        public List<LineItemResponse> items;
        public ShippingAddress shippingAddress;
    }

    public static class LineItemResponse {
        public Long lineItemId;
        public Long skuId;
        public Integer quantity;
        public Long unitPriceCents;
        public Long totalPriceCents;
        public String displayName;
        public String itemType;
    }
}
