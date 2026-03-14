package com.hmall.activity.domain;

import java.time.LocalDate;
import java.util.List;

public record OrderFactStats(
    int totalOrders,
    int completedOrders,
    int cancelledOrders,
    int inProgressOrders,
    int cancelByTimeout,
    int cancelByManual,
    int ordersWithEngraving,
    int ordersWithWarranty,
    int ordersWithAnyVas,
    int multiItemOrders,
    long totalRevenueCents,
    long avgOrderAmountCents,
    long avgVasOrderAmountCents,
    long avgNonVasOrderAmountCents,
    long avgPaymentDurationSec,
    long avgFulfillmentDurationSec,
    long distinctBuyerCount,
    long repeatBuyerCount,
    LocalDate from,
    LocalDate to
) {
    public static OrderFactStats compute(List<OrderFact> facts, LocalDate from, LocalDate to) {
        int total = facts.size();
        int completed = 0, cancelled = 0, inProgress = 0;
        int cancelTimeout = 0, cancelManual = 0;
        int withEngraving = 0, withWarranty = 0, withAnyVas = 0, multiItem = 0;
        long totalRevenue = 0;
        long vasRevenue = 0, nonVasRevenue = 0;
        int vasCount = 0, nonVasCount = 0;
        long totalPaymentDur = 0;
        int paymentDurCount = 0;
        long totalFulfillmentDur = 0;
        int fulfillmentDurCount = 0;

        java.util.Map<Long, Integer> buyerOrders = new java.util.HashMap<>();

        for (OrderFact f : facts) {
            totalRevenue += f.totalAmountCents();

            if ("COMPLETED".equals(f.currentStage())) completed++;
            else if ("CANCELLED".equals(f.currentStage())) cancelled++;
            else inProgress++;

            if ("TIMEOUT".equals(f.cancelReason())) cancelTimeout++;
            else if ("MANUAL".equals(f.cancelReason())) cancelManual++;

            if (f.hasEngraving()) withEngraving++;
            if (f.hasWarranty()) withWarranty++;
            boolean anyVas = f.hasEngraving() || f.hasWarranty();
            if (anyVas) {
                withAnyVas++;
                vasRevenue += f.totalAmountCents();
                vasCount++;
            } else {
                nonVasRevenue += f.totalAmountCents();
                nonVasCount++;
            }

            if (f.itemCount() > 1) multiItem++;

            if (f.paymentDurationSec() != null) {
                totalPaymentDur += f.paymentDurationSec();
                paymentDurCount++;
            }
            if (f.fulfillmentDurationSec() != null) {
                totalFulfillmentDur += f.fulfillmentDurationSec();
                fulfillmentDurCount++;
            }

            if (f.userId() != null) {
                buyerOrders.merge(f.userId(), 1, Integer::sum);
            }
        }

        long avgAmount = total > 0 ? totalRevenue / total : 0;
        long avgVas = vasCount > 0 ? vasRevenue / vasCount : 0;
        long avgNonVas = nonVasCount > 0 ? nonVasRevenue / nonVasCount : 0;
        long avgPayDur = paymentDurCount > 0 ? totalPaymentDur / paymentDurCount : 0;
        long avgFulDur = fulfillmentDurCount > 0 ? totalFulfillmentDur / fulfillmentDurCount : 0;
        long distinctBuyers = buyerOrders.size();
        long repeatBuyers = buyerOrders.values().stream().filter(c -> c > 1).count();

        return new OrderFactStats(total, completed, cancelled, inProgress,
            cancelTimeout, cancelManual, withEngraving, withWarranty, withAnyVas, multiItem,
            totalRevenue, avgAmount, avgVas, avgNonVas, avgPayDur, avgFulDur,
            distinctBuyers, repeatBuyers, from, to);
    }
}
