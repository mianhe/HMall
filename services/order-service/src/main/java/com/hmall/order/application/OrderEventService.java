package com.hmall.order.application;

import com.hmall.order.application.event.ItemSnapshot;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.PricingSnapshot;
import com.hmall.order.application.port.CouponLifecyclePort;
import com.hmall.order.application.port.CreateFulfillmentPort;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import com.hmall.order.application.port.ReleaseInventoryPort;
import com.hmall.order.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/** 处理 Order 订阅的外部事件，更新订单状态并触发下游动作。 */
@Service
public class OrderEventService {

    private static final Logger log = LoggerFactory.getLogger(OrderEventService.class);

    private final OrderRepository orderRepository;
    private final CreateFulfillmentPort createFulfillmentPort;
    private final ReleaseInventoryPort releaseInventoryPort;
    private final CouponLifecyclePort couponLifecyclePort;
    private final OrderOutboundEventPublisher outboundEventPublisher;
    private final TransactionTemplate txTemplate;

    public OrderEventService(
            OrderRepository orderRepository,
            CreateFulfillmentPort createFulfillmentPort,
            ReleaseInventoryPort releaseInventoryPort,
            CouponLifecyclePort couponLifecyclePort,
            OrderOutboundEventPublisher outboundEventPublisher,
            TransactionTemplate txTemplate) {
        this.orderRepository = orderRepository;
        this.createFulfillmentPort = createFulfillmentPort;
        this.releaseInventoryPort = releaseInventoryPort;
        this.couponLifecyclePort = couponLifecyclePort;
        this.outboundEventPublisher = outboundEventPublisher;
        this.txTemplate = txTemplate;
    }

    /**
     * 先提交 PAID 状态再调用 fulfillment，避免长事务导致 Lost Update：
     * fulfillment 同步返回时可能已发布 ServiceActivated 事件，
     * 若 PAID 事务未提交，ServiceActivated 写入的 serviceActivated=true 会被覆盖。
     */
    public void onPaymentCompleted(Long orderId, Long paymentId) {
        Order order = txTemplate.execute(status -> {
            Order o = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
            updateOrderStatus(o, OrderStatus.PAID);
            return o;
        });

        List<CreateFulfillmentPort.ItemQuantity> items = order.getItems().stream()
                .map(li -> new CreateFulfillmentPort.ItemQuantity(
                    li.getSkuId(), li.getQuantity(), li.getItemType().name(),
                    li.getRelatedSkuId(), li.getServiceAttributes()))
                .toList();
        CreateFulfillmentPort.ShippingAddress addr = new CreateFulfillmentPort.ShippingAddress(
                order.getShippingAddress().recipientName(),
                order.getShippingAddress().phone(),
                order.getShippingAddress().province(),
                order.getShippingAddress().city(),
                order.getShippingAddress().district(),
                order.getShippingAddress().detail());

        if (order.getCouponId() != null) {
            couponLifecyclePort.redeem(order.getCouponId());
        }

        createFulfillmentPort.createFulfillment(orderId, items, addr);
    }

    @Transactional
    public void onFulfillmentOrderAllocated(Long orderId) {
        updateOrderStatus(orderId, OrderStatus.FULFILLING);
    }

    /**
     * 支付失败不取消订单，保持 PENDING_PAYMENT，用户可重试支付。
     * 仅 PaymentExpired（超时）才触发取消与补偿。
     */
    public void onPaymentFailed(Long orderId) {
    }

    @Transactional
    public void onPaymentExpired(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        releaseInventoryPort.release(orderId);
        if (order.getCouponId() != null) {
            couponLifecyclePort.release(order.getCouponId());
        }
        updateOrderStatus(order, OrderStatus.CANCELLED);
    }

    @Transactional
    public void onFulfillmentShipped(Long orderId) {
        updateOrderStatus(orderId, OrderStatus.SHIPPED);
    }

    @Transactional
    public void onFulfillmentDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        Order updated = updateOrder(order, OrderStatus.DELIVERED, true, order.isServiceActivated());
        boolean hasService = updated.hasServiceItems();
        boolean activated = updated.isServiceActivated();
        boolean serviceReady = !hasService || activated;
        log.info("onFulfillmentDelivered: orderId={}, hasServiceItems={}, serviceActivated={}, serviceReady={}",
                orderId, hasService, activated, serviceReady);
        if (serviceReady) {
            List<ItemSnapshot> snapshots = updated.getItems().stream()
                    .map(li -> new ItemSnapshot(li.getSkuId(), li.getSpuId(), li.getQuantity(), li.getUnitPriceCents()))
                    .toList();
            PricingSnapshot pricingSnapshot = buildPricingSnapshot(updated);
            outboundEventPublisher.publish(new OrderCompletedEvent(orderId, updated.getUserId(),
                    updated.getTotalAmountCents(), updated.getCouponId(), pricingSnapshot, snapshots));
        } else {
            log.info("onFulfillmentDelivered: orderId={}, 等待 ServiceActivated 到达后再发布 OrderCompleted", orderId);
        }
    }

    @Transactional
    public void onServiceActivated(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        OrderStatus newStatus = order.hasPhysicalItems() ? order.getStatus() : OrderStatus.DELIVERED;
        Order updated = updateOrder(order, newStatus, order.isPhysicalDelivered(), true);
        boolean hasPhysical = updated.hasPhysicalItems();
        boolean delivered = updated.isPhysicalDelivered();
        boolean physicalReady = !hasPhysical || delivered;
        log.info("onServiceActivated: orderId={}, hasPhysicalItems={}, physicalDelivered={}, physicalReady={}",
                orderId, hasPhysical, delivered, physicalReady);
        if (physicalReady) {
            List<ItemSnapshot> snapshots = updated.getItems().stream()
                    .map(li -> new ItemSnapshot(li.getSkuId(), li.getSpuId(), li.getQuantity(), li.getUnitPriceCents()))
                    .toList();
            PricingSnapshot pricingSnapshot = buildPricingSnapshot(updated);
            outboundEventPublisher.publish(new OrderCompletedEvent(orderId, updated.getUserId(),
                    updated.getTotalAmountCents(), updated.getCouponId(), pricingSnapshot, snapshots));
        } else {
            log.info("onServiceActivated: orderId={}, 等待 FulfillmentDelivered 到达后再发布 OrderCompleted", orderId);
        }
    }

    private void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        updateOrderStatus(order, newStatus);
    }

    private void updateOrderStatus(Order order, OrderStatus newStatus) {
        updateOrder(order, newStatus, order.isPhysicalDelivered(), order.isServiceActivated());
    }

    private Order updateOrder(Order order, OrderStatus newStatus, boolean physicalDelivered, boolean serviceActivated) {
        Order updated = new Order(
            order.getOrderId(),
            order.getUserId(),
            newStatus,
            order.getTotalAmountCents(),
            order.getShippingAddress(),
            order.getItems(),
            physicalDelivered,
            serviceActivated,
            order.getCreatedAt(),
            Instant.now(),
            order.getCouponId()
        );
        return orderRepository.save(updated);
    }

    private PricingSnapshot buildPricingSnapshot(Order order) {
        long originalAmountCents = order.getItems().stream()
            .mapToLong(OrderLineItem::getTotalPriceCents)
            .sum();
        long activityDiscountAmountCents = order.getItems().stream()
            .flatMap(li -> li.getDiscounts().stream())
            .filter(d -> "ACTIVITY".equalsIgnoreCase(d.type()))
            .mapToLong(DiscountDetail::amountCents)
            .sum();
        long couponDiscountAmountCents = order.getItems().stream()
            .flatMap(li -> li.getDiscounts().stream())
            .filter(d -> "COUPON".equalsIgnoreCase(d.type()))
            .mapToLong(DiscountDetail::amountCents)
            .sum();
        long discountAmountCents = order.getItems().stream()
            .flatMap(li -> li.getDiscounts().stream())
            .mapToLong(DiscountDetail::amountCents)
            .sum();
        return new PricingSnapshot(
            originalAmountCents,
            activityDiscountAmountCents,
            couponDiscountAmountCents,
            discountAmountCents,
            order.getTotalAmountCents()
        );
    }
}
