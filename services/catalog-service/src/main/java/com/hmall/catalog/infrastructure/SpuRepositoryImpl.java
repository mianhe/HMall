package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.Spu;
import com.hmall.catalog.domain.SpuRepository;
import com.hmall.catalog.infrastructure.persistence.SpuEntity;
import com.hmall.catalog.infrastructure.persistence.SpuJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SpuRepositoryImpl implements SpuRepository {

    private final SpuJpaRepository jpaRepository;

    public SpuRepositoryImpl(SpuJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Spu save(Spu spu) {
        SpuEntity entity = toEntity(spu);
        SpuEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Spu> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Spu> findByCategoryId(Long categoryId) {
        return jpaRepository.findByCategoryId(categoryId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Spu> searchByName(String keyword) {
        List<SpuEntity> entities = (keyword == null || keyword.isBlank())
            ? jpaRepository.findAll()
            : jpaRepository.searchByNameContaining(keyword);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private SpuEntity toEntity(Spu domain) {
        SpuEntity e = new SpuEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setCategoryId(domain.getCategoryId());
        e.setName(domain.getName());
        e.setDescription(domain.getDescription());
        e.setProductType(domain.getProductType());
        e.setServiceKind(domain.getServiceKind());
        return e;
    }

    private Spu toDomain(SpuEntity entity) {
        return new Spu(
            entity.getId(),
            entity.getCategoryId(),
            entity.getName(),
            entity.getDescription(),
            entity.getProductType(),
            entity.getServiceKind()
        );
    }
}
