package com.hmall.order.application;

import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.port.CreateFulfillmentPort;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import com.hmall.order.application.port.ReleaseInventoryPort;
import com.hmall.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** 处理 Order 订阅的外部事件，更新订单状态并触发下游动作。 */
@Service
public class OrderEventService {

    private final OrderRepository orderRepository;
    private final CreateFulfillmentPort createFulfillmentPort;
    private final ReleaseInventoryPort releaseInventoryPort;
    private final OrderOutboundEventPublisher outboundEventPublisher;

    public OrderEventService(
            OrderRepository orderRepository,
            CreateFulfillmentPort createFulfillmentPort,
            ReleaseInventoryPort releaseInventoryPort,
            OrderOutboundEventPublisher outboundEventPublisher) {
        this.orderRepository = orderRepository;
        this.createFulfillmentPort = createFulfillmentPort;
        this.releaseInventoryPort = releaseInventoryPort;
        this.outboundEventPublisher = outboundEventPublisher;
    }

    @Transactional
    public void onPaymentCompleted(Long orderId, Long paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));

        updateOrderStatus(order, OrderStatus.PAID);

        List<CreateFulfillmentPort.ItemQuantity> items = order.getItems().stream()
                .map(li -> new CreateFulfillmentPort.ItemQuantity(li.getSkuId(), li.getQuantity()))
                .toList();
        CreateFulfillmentPort.ShippingAddress addr = new CreateFulfillmentPort.ShippingAddress(
                order.getShippingAddress().recipientName(),
                order.getShippingAddress().phone(),
                order.getShippingAddress().province(),
                order.getShippingAddress().city(),
                order.getShippingAddress().district(),
                order.getShippingAddress().detail());

        createFulfillmentPort.createFulfillment(orderId, items, addr);
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
        updateOrderStatus(orderId, OrderStatus.DELIVERED);
        outboundEventPublisher.publish(new OrderCompletedEvent(orderId));
    }

    private void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        updateOrderStatus(order, newStatus);
    }

    private void updateOrderStatus(Order order, OrderStatus newStatus) {
        Order updated = new Order(
                order.getOrderId(),
                order.getUserId(),
                newStatus,
                order.getTotalAmountCents(),
                order.getShippingAddress(),
                order.getItems(),
                order.getCreatedAt(),
                Instant.now()
        );
        orderRepository.save(updated);
    }
}
