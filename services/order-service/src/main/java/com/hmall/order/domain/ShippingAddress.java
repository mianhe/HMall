package com.hmall.order.domain;

import java.util.Objects;

public record ShippingAddress(
    String recipientName,
    String phone,
    String province,
    String city,
    String district,
    String detail
) {
    public ShippingAddress {
        Objects.requireNonNull(recipientName, "recipientName");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(province, "province");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(detail, "detail");
    }
}
