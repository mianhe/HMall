package com.hmall.inventory.infrastructure;

import com.hmall.inventory.domain.SkuStock;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.infrastructure.persistence.SkuStockEntity;
import com.hmall.inventory.infrastructure.persistence.SkuStockJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SkuStockRepositoryImpl implements SkuStockRepository {

    private final SkuStockJpaRepository jpaRepository;

    public SkuStockRepositoryImpl(SkuStockJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<SkuStock> findBySkuId(Long skuId) {
        return jpaRepository.findById(skuId).map(this::toDomain);
    }

    @Override
    public SkuStock save(SkuStock stock) {
        SkuStockEntity entity = toEntity(stock);
        SkuStockEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private SkuStockEntity toEntity(SkuStock domain) {
        SkuStockEntity e = new SkuStockEntity();
        e.setSkuId(domain.getSkuId());
        e.setAvailable(domain.getAvailable());
        e.setReserved(domain.getReserved());
        return e;
    }

    private SkuStock toDomain(SkuStockEntity entity) {
        return new SkuStock(entity.getSkuId(), entity.getAvailable(), entity.getReserved());
    }
}
