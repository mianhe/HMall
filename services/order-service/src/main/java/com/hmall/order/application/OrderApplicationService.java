package com.hmall.order.application;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCreatedEvent;
import com.hmall.order.application.port.*;
import com.hmall.order.api.dto.OrderCreateDto;
import com.hmall.order.api.dto.OrderDto;
import com.hmall.order.api.dto.OrderListPageDto;
import com.hmall.order.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final SkuInfoPort skuInfoPort;
    private final UserExistsPort userExistsPort;
    private final OccupyInventoryPort occupyInventoryPort;
    private final CreatePaymentPort createPaymentPort;
    private final ReleaseInventoryPort releaseInventoryPort;
    private final RefundPaymentPort refundPaymentPort;
    private final OrderOutboundEventPublisher outboundEventPublisher;

    public OrderApplicationService(OrderRepository orderRepository, SkuInfoPort skuInfoPort,
            UserExistsPort userExistsPort,
            OccupyInventoryPort occupyInventoryPort, CreatePaymentPort createPaymentPort,
            ReleaseInventoryPort releaseInventoryPort, RefundPaymentPort refundPaymentPort,
            OrderOutboundEventPublisher outboundEventPublisher) {
        this.orderRepository = orderRepository;
        this.skuInfoPort = skuInfoPort;
        this.userExistsPort = userExistsPort;
        this.occupyInventoryPort = occupyInventoryPort;
        this.createPaymentPort = createPaymentPort;
        this.releaseInventoryPort = releaseInventoryPort;
        this.refundPaymentPort = refundPaymentPort;
        this.outboundEventPublisher = outboundEventPublisher;
    }

    @Transactional
    public OrderDto placeOrder(OrderCreateDto dto) {
        userExistsPort.requireExists(dto.userId());

        if (dto.items() == null || dto.items().isEmpty()) {
            throw new OrderBadRequestException("商品明细不能为空");
        }

        ShippingAddress address = new ShippingAddress(
            dto.shippingAddress().recipientName(),
            dto.shippingAddress().phone(),
            dto.shippingAddress().province(),
            dto.shippingAddress().city(),
            dto.shippingAddress().district(),
            dto.shippingAddress().detail()
        );

        List<OrderLineItem> items = new ArrayList<>();
        for (OrderCreateDto.LineItemCreateDto line : dto.items()) {
            if (line.quantity() == null || line.quantity() < 1) {
                throw new OrderBadRequestException("商品数量必须大于 0");
            }
            SkuInfoPort.SkuInfo sku = skuInfoPort.getById(line.skuId());
            if (sku.priceCents() < 0) {
                throw new OrderBadRequestException("商品单价不能小于 0");
            }
            items.add(new OrderLineItem(line.skuId(), line.quantity(), sku.priceCents(), sku.displayName()));
        }

        Order order = new Order(dto.userId(), address, items);
        Order saved = orderRepository.save(order);

        List<OccupyInventoryPort.ItemQuantity> occupyItems = items.stream()
                .map(li -> new OccupyInventoryPort.ItemQuantity(li.getSkuId(), li.getQuantity()))
                .collect(Collectors.toList());
        try {
            occupyInventoryPort.occupy(saved.getOrderId(), occupyItems);
        } catch (Exception e) {
            throw new OrderBadRequestException("库存不足");
        }

        try {
            createPaymentPort.createPayment(saved.getOrderId(), saved.getTotalAmountCents());
        } catch (Exception e) {
            releaseInventoryPort.release(saved.getOrderId());
            throw new OrderBadRequestException("创建支付失败");
        }

        outboundEventPublisher.publish(new OrderCreatedEvent(saved.getOrderId(),
                occupyItems.stream()
                        .map(iq -> new OrderCreatedEvent.ItemQuantity(iq.skuId(), iq.quantity()))
                        .toList()));

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderListPageDto listByUserId(Long userId, int page, int size) {
        List<Order> orders = orderRepository.findByUserId(userId, page, size);
        long total = orderRepository.countByUserId(userId);
        List<OrderDto> content = orders.stream().map(this::toDto).toList();
        return new OrderListPageDto(content, total);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new OrderBadRequestException("订单状态不允许取消");
        }

        releaseInventoryPort.release(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            refundPaymentPort.refund(orderId);
        }

        Order cancelled = new Order(
                order.getOrderId(),
                order.getUserId(),
                OrderStatus.CANCELLED,
                order.getTotalAmountCents(),
                order.getShippingAddress(),
                order.getItems(),
                order.getCreatedAt(),
                Instant.now()
        );
        orderRepository.save(cancelled);
        outboundEventPublisher.publish(new OrderCancelledEvent(orderId));
    }

    private OrderDto toDto(Order order) {
        List<OrderDto.OrderLineItemDto> itemDtos = order.getItems().stream()
            .map(li -> {
                String displayName = resolveDisplayName(li.getSkuId(), li.getDisplayName());
                return new OrderDto.OrderLineItemDto(
                    li.getLineItemId(), li.getSkuId(), li.getQuantity(),
                    li.getUnitPriceCents(), li.getTotalPriceCents(), displayName
                );
            })
            .toList();
        OrderCreateDto.ShippingAddressDto addr = new OrderCreateDto.ShippingAddressDto(
            order.getShippingAddress().recipientName(),
            order.getShippingAddress().phone(),
            order.getShippingAddress().province(),
            order.getShippingAddress().city(),
            order.getShippingAddress().district(),
            order.getShippingAddress().detail()
        );
        return new OrderDto(
            order.getOrderId(),
            order.getStatus().name(),
            order.getTotalAmountCents(),
            itemDtos,
            addr,
            order.getCreatedAt()
        );
    }

    /** 查询时用 Catalog 补全展示名（商品名 + SKU），历史订单也会显示完整名称 */
    private String resolveDisplayName(Long skuId, String storedDisplayName) {
        try {
            SkuInfoPort.SkuInfo sku = skuInfoPort.getById(skuId);
            if (sku != null && sku.displayName() != null && !sku.displayName().isBlank()) {
                return sku.displayName();
            }
        } catch (Exception ignored) {
            // Catalog 不可用或 SKU 已删除时沿用存储的展示名
        }
        return storedDisplayName != null ? storedDisplayName : "商品";
    }
}
