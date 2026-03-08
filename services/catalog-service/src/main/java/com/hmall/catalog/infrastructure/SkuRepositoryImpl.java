package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.SkuRepository;
import com.hmall.catalog.infrastructure.persistence.SkuEntity;
import com.hmall.catalog.infrastructure.persistence.SkuJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SkuSpecValueEntity;
import com.hmall.catalog.infrastructure.persistence.SkuSpecValueJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public boolean existsBySpecOptionId(Long optionId) {
        return specValueJpaRepository.existsBySpecOptionId(optionId);
    }

    @Override
    public void deleteById(Long id) {
        specValueJpaRepository.deleteBySkuId(id);
        skuJpaRepository.deleteById(id);
    }

    @Override
    public List<Sku> findBySpuId(Long spuId) {
        List<SkuEntity> entities = skuJpaRepository.findBySpuIdOrderByIdAsc(spuId);
        return batchToDomain(entities);
    }

    @Override
    public List<Sku> findBySpuIdIn(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) return List.of();
        List<SkuEntity> entities = skuJpaRepository.findBySpuIdInOrderBySpuIdAscIdAsc(spuIds);
        return batchToDomain(entities);
    }

    private List<Sku> batchToDomain(List<SkuEntity> entities) {
        if (entities.isEmpty()) return List.of();
        List<Long> skuIds = entities.stream().map(SkuEntity::getId).toList();
        Map<Long, List<Long>> optionsBySkuId = new LinkedHashMap<>();
        for (SkuSpecValueEntity sv : specValueJpaRepository.findBySkuIdInOrderBySkuIdAscIdAsc(skuIds)) {
            optionsBySkuId.computeIfAbsent(sv.getSkuId(), k -> new ArrayList<>())
                .add(sv.getSpecOptionId());
        }
        return entities.stream()
            .map(e -> toDomain(e, optionsBySkuId.getOrDefault(e.getId(), List.of())))
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
