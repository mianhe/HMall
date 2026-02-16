package com.hmall.inventory.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 实体：一次占用记录，用于按 orderId 释放。
 */
public class Reservation {

    private final Long reservationId;
    private final Long orderId;
    private final Long skuId;
    private final int quantity;
    private ReservationStatus status;
    private final Instant createdAt;

    /** 新建占用记录 */
    public Reservation(Long orderId, Long skuId, int quantity) {
        this.reservationId = null;
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.skuId = Objects.requireNonNull(skuId, "skuId");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
        this.createdAt = Instant.now();
    }

    /** 从持久化还原 */
    public Reservation(Long reservationId, Long orderId, Long skuId, int quantity,
                       ReservationStatus status, Instant createdAt) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
