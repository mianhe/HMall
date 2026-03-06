package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.EngravingPattern;
import com.hmall.catalog.domain.EngravingPatternRepository;
import com.hmall.catalog.infrastructure.persistence.EngravingPatternEntity;
import com.hmall.catalog.infrastructure.persistence.EngravingPatternJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EngravingPatternRepositoryImpl implements EngravingPatternRepository {

    private final EngravingPatternJpaRepository jpaRepository;

    public EngravingPatternRepositoryImpl(EngravingPatternJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EngravingPattern save(EngravingPattern pattern) {
        EngravingPatternEntity entity = toEntity(pattern);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<EngravingPattern> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<EngravingPattern> findAllOrderBySortOrder(Boolean enabledFilter) {
        List<EngravingPatternEntity> entities = Boolean.TRUE.equals(enabledFilter)
            ? jpaRepository.findByEnabledTrueOrderBySortOrderAsc()
            : jpaRepository.findAllByOrderBySortOrderAsc();
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private EngravingPatternEntity toEntity(EngravingPattern p) {
        EngravingPatternEntity e = new EngravingPatternEntity();
        if (p.getId() != null) e.setId(p.getId());
        e.setName(p.getName());
        e.setImageUrl(p.getImageUrl());
        e.setSortOrder(p.getSortOrder());
        e.setEnabled(p.isEnabled());
        return e;
    }

    private EngravingPattern toDomain(EngravingPatternEntity e) {
        return new EngravingPattern(e.getId(), e.getName(), e.getImageUrl(), e.getSortOrder(), e.isEnabled());
    }
}
