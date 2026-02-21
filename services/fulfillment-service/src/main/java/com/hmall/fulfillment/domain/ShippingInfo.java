package com.hmall.fulfillment.domain;

import java.time.Instant;
import java.util.Objects;

public class ShippingInfo {

    private final String carrier;
    private final String trackingNumber;
    private final Instant shippedAt;
    private Instant deliveredAt;

    public ShippingInfo(String carrier, String trackingNumber, Instant shippedAt) {
        this.carrier = Objects.requireNonNull(carrier, "carrier");
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "trackingNumber");
        this.shippedAt = Objects.requireNonNull(shippedAt, "shippedAt");
    }

    public ShippingInfo(String carrier, String trackingNumber, Instant shippedAt, Instant deliveredAt) {
        this(carrier, trackingNumber, shippedAt);
        this.deliveredAt = deliveredAt;
    }

    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }

    public void markDelivered(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
