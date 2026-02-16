package com.hmall.inventory.application;

/**
 * 应用层占用项：skuId + quantity。
 */
public record OccupyItem(Long skuId, int quantity) {}
