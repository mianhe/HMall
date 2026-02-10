package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.SkuRepository;
import com.hmall.catalog.infrastructure.persistence.SkuEntity;
import com.hmall.catalog.infrastructure.persistence.SkuJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SkuSpecValueEntity;
import com.hmall.catalog.infrastructure.persistence.SkuSpecValueJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuJpaRepository skuJpaRepository;
    private final SkuSpecValueJpaRepository specValueJpaRepository;

    public SkuRepositoryImpl(SkuJpaRepository skuJpaRepository, SkuSpecValueJpaRepository specValueJpaRepository) {
        this.skuJpaRepository = skuJpaRepository;
        this.specValueJpaRepository = specValueJpaRepository;
    }

    @Override
    public Sku save(Sku sku) {
        SkuEntity entity = toEntity(sku);
        SkuEntity saved = skuJpaRepository.save(entity);
        if (sku.getId() != null) {
            specValueJpaRepository.deleteBySkuId(saved.getId());
        }
        for (Long optionId : sku.getSpecOptionIds()) {
            SkuSpecValueEntity sv = new SkuSpecValueEntity();
            sv.setSkuId(saved.getId());
            sv.setSpecOptionId(optionId);
            specValueJpaRepository.save(sv);
        }
        return toDomain(saved);
    }

    @Override
    public Optional<Sku> findById(Long id) {
        return skuJpaRepository.findById(id)
            .map(entity -> {
                List<Long> optionIds = specValueJpaRepository.findBySkuIdOrderByIdAsc(entity.getId()).stream()
                    .map(SkuSpecValueEntity::getSpecOptionId)
                    .toList();
                return toDomain(entity, optionIds);
            });
    }

    @Override
    public List<Sku> findBySpuId(Long spuId) {
        return skuJpaRepository.findBySpuIdOrderByIdAsc(spuId).stream()
            .map(entity -> {
                List<Long> optionIds = specValueJpaRepository.findBySkuIdOrderByIdAsc(entity.getId()).stream()
                    .map(SkuSpecValueEntity::getSpecOptionId)
                    .toList();
                return toDomain(entity, optionIds);
            })
            .toList();
    }

    private SkuEntity toEntity(Sku domain) {
        SkuEntity e = new SkuEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setSpuId(domain.getSpuId());
        e.setDisplayName(domain.getDisplayName());
        e.setPriceCents(domain.getPriceCents());
        return e;
    }

    private Sku toDomain(SkuEntity entity) {
        List<Long> optionIds = specValueJpaRepository.findBySkuIdOrderByIdAsc(entity.getId()).stream()
            .map(SkuSpecValueEntity::getSpecOptionId)
            .toList();
        return toDomain(entity, optionIds);
    }

    private Sku toDomain(SkuEntity entity, List<Long> optionIds) {
        return new Sku(
            entity.getId(),
            entity.getSpuId(),
            entity.getDisplayName(),
            entity.getPriceCents() != null ? entity.getPriceCents() : 0L,
            optionIds
        );
    }
}
