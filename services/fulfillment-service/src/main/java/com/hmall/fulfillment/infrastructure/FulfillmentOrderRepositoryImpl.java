package com.hmall.fulfillment.infrastructure;

import com.hmall.fulfillment.domain.EngravingInfo;
import com.hmall.fulfillment.domain.FulfillmentItem;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.domain.ShippingAddress;
import com.hmall.fulfillment.domain.ShippingInfo;
import com.hmall.fulfillment.infrastructure.persistence.FulfillmentItemEntity;
import com.hmall.fulfillment.infrastructure.persistence.FulfillmentOrderEntity;
import com.hmall.fulfillment.infrastructure.persistence.FulfillmentOrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FulfillmentOrderRepositoryImpl implements FulfillmentOrderRepository {

    private final FulfillmentOrderJpaRepository jpaRepository;

    public FulfillmentOrderRepositoryImpl(FulfillmentOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FulfillmentOrder save(FulfillmentOrder order) {
        FulfillmentOrderEntity entity = toEntity(order);
        FulfillmentOrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FulfillmentOrder> findById(Long fulfillmentOrderId) {
        return jpaRepository.findById(fulfillmentOrderId).map(this::toDomain);
    }

    @Override
    public List<FulfillmentOrder> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<FulfillmentOrder> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private FulfillmentOrderEntity toEntity(FulfillmentOrder domain) {
        FulfillmentOrderEntity e = new FulfillmentOrderEntity();
        if (domain.getFulfillmentOrderId() != null) {
            e.setId(domain.getFulfillmentOrderId());
        }
        e.setOrderId(domain.getOrderId());
        e.setFulfillmentType(domain.getFulfillmentType());
        e.setStatus(domain.getStatus());
        e.setRecipientName(domain.getShippingAddress().getRecipientName());
        e.setPhone(domain.getShippingAddress().getPhone());
        e.setProvince(domain.getShippingAddress().getProvince());
        e.setCity(domain.getShippingAddress().getCity());
        e.setDistrict(domain.getShippingAddress().getDistrict());
        e.setDetail(domain.getShippingAddress().getDetail());
        if (domain.getShippingInfo() != null) {
            e.setCarrier(domain.getShippingInfo().getCarrier());
            e.setTrackingNumber(domain.getShippingInfo().getTrackingNumber());
            e.setShippedAt(domain.getShippingInfo().getShippedAt());
            e.setDeliveredAt(domain.getShippingInfo().getDeliveredAt());
        }
        if (domain.getEngravingInfo() != null) {
            e.setEngravingPatternId(domain.getEngravingInfo().getPatternId());
            e.setEngravingPatternName(domain.getEngravingInfo().getPatternName());
            e.setEngravingText(domain.getEngravingInfo().getText());
        }
        e.setEngravingCompletedAt(domain.getEngravingCompletedAt());
        e.setCreatedAt(domain.getCreatedAt());
        e.setUpdatedAt(domain.getUpdatedAt());

        List<FulfillmentItemEntity> itemEntities = domain.getItems().stream().map(item -> {
            FulfillmentItemEntity ie = new FulfillmentItemEntity();
            if (item.getFulfillmentItemId() != null) {
                ie.setId(item.getFulfillmentItemId());
            }
            ie.setSkuId(item.getSkuId());
            ie.setQuantity(item.getQuantity());
            ie.setItemType(item.getItemType());
            ie.setFulfillmentOrder(e);
            return ie;
        }).toList();
        e.setItems(new ArrayList<>(itemEntities));

        return e;
    }

    private FulfillmentOrder toDomain(FulfillmentOrderEntity entity) {
        ShippingAddress address = new ShippingAddress(
            entity.getRecipientName(), entity.getPhone(),
            entity.getProvince(), entity.getCity(), entity.getDistrict(), entity.getDetail()
        );

        ShippingInfo shippingInfo = null;
        if (entity.getCarrier() != null) {
            shippingInfo = new ShippingInfo(
                entity.getCarrier(), entity.getTrackingNumber(),
                entity.getShippedAt(), entity.getDeliveredAt()
            );
        }

        List<FulfillmentItem> items = entity.getItems().stream()
            .map(ie -> new FulfillmentItem(ie.getId(), ie.getSkuId(), ie.getQuantity(), ie.getItemType()))
            .toList();

        EngravingInfo engravingInfo = null;
        if (entity.getEngravingPatternId() != null || (entity.getEngravingText() != null && !entity.getEngravingText().isBlank())) {
            engravingInfo = new EngravingInfo(entity.getEngravingPatternId(), entity.getEngravingPatternName(), entity.getEngravingText());
        }

        return new FulfillmentOrder(
            entity.getId(), entity.getOrderId(), entity.getFulfillmentType(), entity.getStatus(),
            items, address, shippingInfo, engravingInfo, entity.getEngravingCompletedAt(),
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
