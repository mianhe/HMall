package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecDimensionRepository;
import com.hmall.catalog.infrastructure.persistence.SpecDimensionEntity;
import com.hmall.catalog.infrastructure.persistence.SpecDimensionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SpecDimensionRepositoryImpl implements SpecDimensionRepository {

    private final SpecDimensionJpaRepository jpaRepository;

    public SpecDimensionRepositoryImpl(SpecDimensionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SpecDimension save(SpecDimension dimension) {
        SpecDimensionEntity entity = toEntity(dimension);
        SpecDimensionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SpecDimension> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SpecDimension> findBySpuId(Long spuId) {
        return jpaRepository.findBySpuIdOrderBySortOrderAscIdAsc(spuId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsBySpuIdAndName(Long spuId, String name) {
        return jpaRepository.existsBySpuIdAndName(spuId, name);
    }

    private SpecDimensionEntity toEntity(SpecDimension domain) {
        SpecDimensionEntity e = new SpecDimensionEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setSpuId(domain.getSpuId());
        e.setName(domain.getName());
        e.setRequired(domain.isRequired());
        e.setSortOrder(domain.getSortOrder());
        return e;
    }

    private SpecDimension toDomain(SpecDimensionEntity entity) {
        return new SpecDimension(
            entity.getId(),
            entity.getSpuId(),
            entity.getName(),
            Boolean.TRUE.equals(entity.getRequired()),
            entity.getSortOrder()
        );
    }
}
