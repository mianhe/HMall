package com.hmall.order.api.dto;

import java.util.List;

public record OrderListPageDto(
    List<OrderDto> content,
    long totalElements
) {}
