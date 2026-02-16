package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByParentIdIsNull();

    List<CategoryEntity> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);
}
