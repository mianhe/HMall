package com.hmall.activity.application;

import java.time.LocalDate;

public record SeedRequest(
    LocalDate startDate,
    LocalDate endDate,
    int ordersPerDay,
    int maxOrders,
    String batchTag,
    Double cancelRatio,
    Double paymentExpiredRatio,
    Double paymentFailedRatio,
    Double engravingRatio,
    Double warrantyRatio,
    Double multiItemRatio
) {
    private static final double DEFAULT_CANCEL = 0.08;
    private static final double DEFAULT_PAYMENT_EXPIRED = 0.04;
    private static final double DEFAULT_PAYMENT_FAILED = 0.03;
    private static final double DEFAULT_ENGRAVING = 0.10;
    private static final double DEFAULT_WARRANTY = 0.10;
    private static final double DEFAULT_MULTI_ITEM = 0.25;

    public double cancelRatioOrDefault() { return cancelRatio != null ? cancelRatio : DEFAULT_CANCEL; }
    public double paymentExpiredRatioOrDefault() { return paymentExpiredRatio != null ? paymentExpiredRatio : DEFAULT_PAYMENT_EXPIRED; }
    public double paymentFailedRatioOrDefault() { return paymentFailedRatio != null ? paymentFailedRatio : DEFAULT_PAYMENT_FAILED; }
    public double engravingRatioOrDefault() { return engravingRatio != null ? engravingRatio : DEFAULT_ENGRAVING; }
    public double warrantyRatioOrDefault() { return warrantyRatio != null ? warrantyRatio : DEFAULT_WARRANTY; }
    public double multiItemRatioOrDefault() { return multiItemRatio != null ? multiItemRatio : DEFAULT_MULTI_ITEM; }
}
