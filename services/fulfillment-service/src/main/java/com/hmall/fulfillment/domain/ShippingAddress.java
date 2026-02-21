package com.hmall.fulfillment.domain;

import java.util.Objects;

public class ShippingAddress {

    private final String recipientName;
    private final String phone;
    private final String province;
    private final String city;
    private final String district;
    private final String detail;

    public ShippingAddress(String recipientName, String phone,
                           String province, String city, String district, String detail) {
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.province = Objects.requireNonNull(province, "province");
        this.city = Objects.requireNonNull(city, "city");
        this.district = Objects.requireNonNull(district, "district");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getDetail() { return detail; }
}
