package com.hmall.user.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateDto(
    @NotBlank(message = "收件人不能为空") String recipientName,
    @NotBlank(message = "手机号不能为空") String phone,
    @NotBlank(message = "省/直辖市不能为空") String province,
    @NotBlank(message = "城市不能为空") String city,
    @NotBlank(message = "区/县不能为空") String district,
    @NotBlank(message = "详细地址不能为空") String detail
) {}
