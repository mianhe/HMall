package com.hmall.promotion.api.dto;

import jakarta.validation.constraints.NotNull;

public record LockCouponRequest(@NotNull Long orderId) {}
