package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.SpecOption;
import com.hmall.catalog.domain.SpecOptionRepository;
import com.hmall.catalog.infrastructure.persistence.SpecOptionEntity;
import com.hmall.catalog.infrastructure.persistence.SpecOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SpecOptionRepositoryImpl implements SpecOptionRepository {

    private final SpecOptionJpaRepository jpaRepository;

    public SpecOptionRepositoryImpl(SpecOptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SpecOption save(SpecOption option) {
        SpecOptionEntity entity = toEntity(option);
        SpecOptionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SpecOption> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SpecOption> findBySpecDimensionId(Long specDimensionId) {
        return jpaRepository.findBySpecDimensionIdOrderBySortOrderAscIdAsc(specDimensionId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsBySpecDimensionIdAndOptionValue(Long specDimensionId, String optionValue) {
        return jpaRepository.existsBySpecDimensionIdAndOptionValue(specDimensionId, optionValue);
    }

    private SpecOptionEntity toEntity(SpecOption domain) {
        SpecOptionEntity e = new SpecOptionEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setSpecDimensionId(domain.getSpecDimensionId());
        e.setOptionValue(domain.getOptionValue());
        e.setSortOrder(domain.getSortOrder());
        e.setImage(domain.getImage());
        return e;
    }

    private SpecOption toDomain(SpecOptionEntity entity) {
        return new SpecOption(
            entity.getId(),
            entity.getSpecDimensionId(),
            entity.getOptionValue(),
            entity.getSortOrder(),
            entity.getImage()
        );
    }
}
