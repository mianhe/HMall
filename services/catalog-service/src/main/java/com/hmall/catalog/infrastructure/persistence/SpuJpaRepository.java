package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpuJpaRepository extends JpaRepository<SpuEntity, Long> {

    List<SpuEntity> findByCategoryId(Long categoryId);

    @Query("SELECT s FROM SpuEntity s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SpuEntity> searchByNameContaining(@Param("keyword") String keyword);
}
