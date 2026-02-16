package com.hmall.user.api.dto;

import com.hmall.user.domain.Address;

public record AddressDto(
    Long addressId,
    Long userId,
    String recipientName,
    String phone,
    String province,
    String city,
    String district,
    String detail
) {
    public static AddressDto from(Address a) {
        return new AddressDto(
            a.getAddressId(),
            a.getUserId(),
            a.getRecipientName(),
            a.getPhone(),
            a.getProvince(),
            a.getCity(),
            a.getDistrict(),
            a.getDetail()
        );
    }
}
