package com.hmall.order.domain;

public record DiscountDetail(String type, Long sourceId, long amountCents, String description) {}
