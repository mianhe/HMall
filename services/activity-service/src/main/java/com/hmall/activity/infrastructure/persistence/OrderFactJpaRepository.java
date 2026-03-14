package com.hmall.activity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderFactJpaRepository extends JpaRepository<OrderFactEntity, Long> {

    @Query("SELECT e FROM OrderFactEntity e WHERE e.createdDate >= :from AND e.createdDate <= :to")
    List<OrderFactEntity> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
