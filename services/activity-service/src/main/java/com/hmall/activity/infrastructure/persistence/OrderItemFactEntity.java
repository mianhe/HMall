package com.hmall.activity.infrastructure.persistence;

import com.hmall.activity.domain.OrderItemFact;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "order_item_fact")
public class OrderItemFactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private Long userId;
    private Long skuId;
    private Long spuId;
    private int quantity;
    private long unitPriceCents;
    private long lineTotalCents;
    private long orderTotalAmountCents;

    @Column(length = 32, nullable = false)
    private String orderCurrentStage;

    private boolean orderHasEngraving;
    private boolean orderHasWarranty;
    private LocalDate createdDate;

    @Column(length = 64)
    private String seedBatch;

    public static OrderItemFactEntity from(OrderItemFact item) {
        OrderItemFactEntity e = new OrderItemFactEntity();
        e.orderId = item.orderId();
        e.userId = item.userId();
        e.skuId = item.skuId();
        e.spuId = item.spuId();
        e.quantity = item.quantity();
        e.unitPriceCents = item.unitPriceCents();
        e.lineTotalCents = item.lineTotalCents();
        e.orderTotalAmountCents = item.orderTotalAmountCents();
        e.orderCurrentStage = item.orderCurrentStage();
        e.orderHasEngraving = item.orderHasEngraving();
        e.orderHasWarranty = item.orderHasWarranty();
        e.createdDate = item.createdDate();
        e.seedBatch = item.seedBatch();
        return e;
    }

    public OrderItemFact toDomain() {
        return new OrderItemFact(id, orderId, userId, skuId, spuId, quantity, unitPriceCents,
            lineTotalCents, orderTotalAmountCents, orderCurrentStage, orderHasEngraving,
            orderHasWarranty, createdDate, seedBatch);
    }
}
