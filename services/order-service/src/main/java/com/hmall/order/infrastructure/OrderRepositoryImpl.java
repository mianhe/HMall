package com.hmall.order.infrastructure;

import com.hmall.order.domain.*;
import com.hmall.order.infrastructure.persistence.OrderEntity;
import com.hmall.order.infrastructure.persistence.OrderJpaRepository;
import com.hmall.order.infrastructure.persistence.OrderLineItemEntity;
import com.hmall.order.infrastructure.persistence.OrderLineItemJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderLineItemJpaRepository lineItemJpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository, OrderLineItemJpaRepository lineItemJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
        this.lineItemJpaRepository = lineItemJpaRepository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        if (order.getOrderId() != null) {
            return updateExisting(order);
        }
        OrderEntity entity = toEntity(order);
        OrderEntity saved = orderJpaRepository.save(entity);
        Long orderId = saved.getId();
        for (OrderLineItem item : order.getItems()) {
            OrderLineItemEntity li = toLineItemEntity(item, orderId);
            lineItemJpaRepository.save(li);
        }
        List<OrderLineItemEntity> savedItems = lineItemJpaRepository.findByOrderIdOrderByIdAsc(orderId);
        return toDomain(saved, savedItems);
    }

    private Order updateExisting(Order order) {
        OrderEntity entity = orderJpaRepository.findById(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("订单不存在: " + order.getOrderId()));
        entity.setStatus(order.getStatus().name());
        entity.setUpdatedAt(order.getUpdatedAt());
        OrderEntity saved = orderJpaRepository.save(entity);
        List<OrderLineItemEntity> savedItems = lineItemJpaRepository.findByOrderIdOrderByIdAsc(saved.getId());
        return toDomain(saved, savedItems);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findById(orderId)
                .map(entity -> {
                    List<OrderLineItemEntity> items = lineItemJpaRepository.findByOrderIdOrderByIdAsc(entity.getId());
                    return toDomain(entity, items);
                });
    }

    @Override
    public List<Order> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .getContent()
                .stream()
                .map(entity -> {
                    List<OrderLineItemEntity> items = lineItemJpaRepository.findByOrderIdOrderByIdAsc(entity.getId());
                    return toDomain(entity, items);
                })
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return orderJpaRepository.countByUserId(userId);
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity e = new OrderEntity();
        if (order.getOrderId() != null) {
            e.setId(order.getOrderId());
        }
        e.setUserId(order.getUserId());
        e.setStatus(order.getStatus().name());
        e.setTotalAmountCents(order.getTotalAmountCents());
        ShippingAddress addr = order.getShippingAddress();
        e.setRecipientName(addr.recipientName());
        e.setPhone(addr.phone());
        e.setProvince(addr.province());
        e.setCity(addr.city());
        e.setDistrict(addr.district());
        e.setDetail(addr.detail());
        e.setCreatedAt(order.getCreatedAt());
        e.setUpdatedAt(order.getUpdatedAt());
        return e;
    }

    private OrderLineItemEntity toLineItemEntity(OrderLineItem item, Long orderId) {
        OrderLineItemEntity e = new OrderLineItemEntity();
        if (item.getLineItemId() != null) {
            e.setId(item.getLineItemId());
        }
        e.setOrderId(orderId);
        e.setSkuId(item.getSkuId());
        e.setQuantity(item.getQuantity());
        e.setUnitPriceCents(item.getUnitPriceCents());
        e.setTotalPriceCents(item.getTotalPriceCents());
        e.setDisplayName(item.getDisplayName());
        return e;
    }

    private Order toDomain(OrderEntity entity, List<OrderLineItemEntity> itemEntities) {
        ShippingAddress addr = new ShippingAddress(
            entity.getRecipientName(), entity.getPhone(),
            entity.getProvince(), entity.getCity(), entity.getDistrict(), entity.getDetail()
        );
        List<OrderLineItem> lineItemsWithIds = itemEntities.stream()
            .map(this::toLineItemDomain)
            .toList();
        return new Order(
            entity.getId(), entity.getUserId(),
            OrderStatus.valueOf(entity.getStatus()),
            entity.getTotalAmountCents(),
            addr, lineItemsWithIds,
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private OrderLineItem toLineItemDomain(OrderLineItemEntity e) {
        return new OrderLineItem(
            e.getId(), e.getOrderId(), e.getSkuId(),
            e.getQuantity(), e.getUnitPriceCents().longValue(), e.getTotalPriceCents().longValue(),
            e.getDisplayName()
        );
    }
}
