package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpuJpaRepository extends JpaRepository<SpuEntity, Long> {

    List<SpuEntity> findByCategoryId(Long categoryId);
}
