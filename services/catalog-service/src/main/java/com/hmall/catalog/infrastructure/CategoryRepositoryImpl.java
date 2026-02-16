package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.Category;
import com.hmall.catalog.domain.CategoryRepository;
import com.hmall.catalog.infrastructure.persistence.CategoryEntity;
import com.hmall.catalog.infrastructure.persistence.CategoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = toEntity(category);
        CategoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Category> findByParentId(Long parentId) {
        List<CategoryEntity> list = parentId == null
            ? jpaRepository.findByParentIdIsNull()
            : jpaRepository.findByParentId(parentId);
        return list.stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return jpaRepository.existsByParentId(parentId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private CategoryEntity toEntity(Category domain) {
        CategoryEntity e = new CategoryEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setParentId(domain.getParentId());
        e.setName(domain.getName());
        e.setDescription(domain.getDescription());
        return e;
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(
            entity.getId(),
            entity.getParentId(),
            entity.getName(),
            entity.getDescription()
        );
    }
}