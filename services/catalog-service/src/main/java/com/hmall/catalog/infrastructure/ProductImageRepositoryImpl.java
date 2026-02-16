package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.ProductImage;
import com.hmall.catalog.domain.ProductImageRepository;
import com.hmall.catalog.infrastructure.persistence.ProductImageEntity;
import com.hmall.catalog.infrastructure.persistence.ProductImageJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductImageRepositoryImpl implements ProductImageRepository {

    private final ProductImageJpaRepository jpaRepository;

    public ProductImageRepositoryImpl(ProductImageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProductImage save(ProductImage productImage) {
        ProductImageEntity entity = toEntity(productImage);
        ProductImageEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ProductImage> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProductImage> findBySpuIdAndSpecOptionIdIsNull(Long spuId) {
        return jpaRepository.findBySpuIdAndSpecOptionIdIsNullOrderBySortOrderAscIdAsc(spuId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<ProductImage> findBySpecOptionId(Long specOptionId) {
        return jpaRepository.findBySpecOptionIdOrderBySortOrderAscIdAsc(specOptionId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private ProductImageEntity toEntity(ProductImage domain) {
        ProductImageEntity e = new ProductImageEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setSpuId(domain.getSpuId());
        e.setSpecOptionId(domain.getSpecOptionId());
        e.setImageUrl(domain.getImageUrl());
        e.setSortOrder(domain.getSortOrder());
        return e;
    }

    private ProductImage toDomain(ProductImageEntity entity) {
        return new ProductImage(
            entity.getId(),
            entity.getSpuId(),
            entity.getSpecOptionId(),
            entity.getImageUrl(),
            entity.getSortOrder()
        );
    }
}
