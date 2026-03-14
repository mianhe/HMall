package com.hmall.activity.infrastructure;

import com.hmall.activity.domain.OrderFact;
import com.hmall.activity.domain.OrderFactRepository;
import com.hmall.activity.domain.OrderItemFact;
import com.hmall.activity.infrastructure.persistence.OrderFactEntity;
import com.hmall.activity.infrastructure.persistence.OrderFactJpaRepository;
import com.hmall.activity.infrastructure.persistence.OrderItemFactEntity;
import com.hmall.activity.infrastructure.persistence.OrderItemFactJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class OrderFactRepositoryImpl implements OrderFactRepository {

    private final OrderFactJpaRepository factJpa;
    private final OrderItemFactJpaRepository itemJpa;

    public OrderFactRepositoryImpl(OrderFactJpaRepository factJpa, OrderItemFactJpaRepository itemJpa) {
        this.factJpa = factJpa;
        this.itemJpa = itemJpa;
    }

    @Override
    @Transactional
    public void save(OrderFact fact) {
        factJpa.save(OrderFactEntity.from(fact));
    }

    @Override
    public Optional<OrderFact> findByOrderId(Long orderId) {
        return factJpa.findById(orderId).map(OrderFactEntity::toDomain);
    }

    @Override
    @Transactional
    public void saveItems(List<OrderItemFact> items) {
        List<OrderItemFactEntity> entities = items.stream()
            .map(OrderItemFactEntity::from)
            .toList();
        itemJpa.saveAll(entities);
    }

    @Override
    public List<OrderItemFact> findItemsByOrderId(Long orderId) {
        return itemJpa.findByOrderId(orderId).stream()
            .map(OrderItemFactEntity::toDomain)
            .toList();
    }

    @Override
    public List<OrderFact> findByDateRange(LocalDate from, LocalDate to) {
        return factJpa.findByDateRange(from, to).stream()
            .map(OrderFactEntity::toDomain)
            .toList();
    }

    @Override
    public List<OrderItemFact> findItemsByDateRange(LocalDate from, LocalDate to) {
        return itemJpa.findByDateRange(from, to).stream()
            .map(OrderItemFactEntity::toDomain)
            .toList();
    }

    @Override
    public List<OrderFact> findByFilters(LocalDate from, LocalDate to,
                                         Boolean hasEngraving, Boolean hasWarranty,
                                         String currentStage, Long userId, int limit) {
        Stream<OrderFact> stream = findByDateRange(from, to).stream();
        if (hasEngraving != null) {
            stream = stream.filter(f -> f.hasEngraving() == hasEngraving);
        }
        if (hasWarranty != null) {
            stream = stream.filter(f -> f.hasWarranty() == hasWarranty);
        }
        if (currentStage != null) {
            stream = stream.filter(f -> currentStage.equals(f.currentStage()));
        }
        if (userId != null) {
            stream = stream.filter(f -> userId.equals(f.userId()));
        }
        return stream.limit(limit).toList();
    }

    @Override
    @Transactional
    public void deleteItemsByOrderId(Long orderId) {
        itemJpa.deleteByOrderId(orderId);
    }

    @Override
    @Transactional
    public void deleteByOrderId(Long orderId) {
        itemJpa.deleteByOrderId(orderId);
        factJpa.deleteById(orderId);
    }

    @Override
    @Transactional
    public void deleteAll() {
        itemJpa.deleteAllInBatch();
        factJpa.deleteAllInBatch();
    }
}
