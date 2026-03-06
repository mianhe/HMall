package com.hmall.order.application;

import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.port.CreateFulfillmentPort;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import com.hmall.order.application.port.ReleaseInventoryPort;
import com.hmall.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/** 处理 Order 订阅的外部事件，更新订单状态并触发下游动作。 */
@Service
public class OrderEventService {

    private final OrderRepository orderRepository;
    private final CreateFulfillmentPort createFulfillmentPort;
    private final ReleaseInventoryPort releaseInventoryPort;
    private final OrderOutboundEventPublisher outboundEventPublisher;
    private final TransactionTemplate txTemplate;

    public OrderEventService(
            OrderRepository orderRepository,
            CreateFulfillmentPort createFulfillmentPort,
            ReleaseInventoryPort releaseInventoryPort,
            OrderOutboundEventPublisher outboundEventPublisher,
            TransactionTemplate txTemplate) {
        this.orderRepository = orderRepository;
        this.createFulfillmentPort = createFulfillmentPort;
        this.releaseInventoryPort = releaseInventoryPort;
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
        releaseInventoryPort.release(orderId);
        updateOrderStatus(orderId, OrderStatus.CANCELLED);
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
        boolean serviceReady = !updated.hasServiceItems() || updated.isServiceActivated();
        if (serviceReady) {
            outboundEventPublisher.publish(new OrderCompletedEvent(orderId));
        }
    }

    @Transactional
    public void onServiceActivated(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        OrderStatus newStatus = order.hasPhysicalItems() ? order.getStatus() : OrderStatus.DELIVERED;
        Order updated = updateOrder(order, newStatus, order.isPhysicalDelivered(), true);
        boolean physicalReady = !updated.hasPhysicalItems() || updated.isPhysicalDelivered();
        if (physicalReady) {
            outboundEventPublisher.publish(new OrderCompletedEvent(orderId));
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
            Instant.now()
        );
        return orderRepository.save(updated);
    }
}
