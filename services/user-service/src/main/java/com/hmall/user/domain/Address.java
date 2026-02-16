package com.hmall.user.domain;

import java.util.Objects;

public class Address {

    private final Long addressId;
    private final Long userId;
    private final String recipientName;
    private final String phone;
    private final String province;
    private final String city;
    private final String district;
    private final String detail;

    public Address(Long userId, String recipientName, String phone,
                   String province, String city, String district, String detail) {
        this.addressId = null;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.province = Objects.requireNonNull(province, "province");
        this.city = Objects.requireNonNull(city, "city");
        this.district = Objects.requireNonNull(district, "district");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public Address(Long addressId, Long userId, String recipientName, String phone,
                   String province, String city, String district, String detail) {
        this.addressId = Objects.requireNonNull(addressId, "addressId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.province = Objects.requireNonNull(province, "province");
        this.city = Objects.requireNonNull(city, "city");
        this.district = Objects.requireNonNull(district, "district");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    public Long getAddressId() { return addressId; }
    public Long getUserId() { return userId; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getDetail() { return detail; }
}
