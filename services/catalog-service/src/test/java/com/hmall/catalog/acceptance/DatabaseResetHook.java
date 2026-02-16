package com.hmall.catalog.acceptance;

import com.hmall.catalog.infrastructure.persistence.CategoryJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SpuJpaRepository;
import io.cucumber.java.Before;

/**
 * 每个场景执行前清空类别与商品表，避免场景间数据干扰。由 AcceptanceTestConfig 注册为 Bean。
 * User 已拆至 user-service。
 */
public class DatabaseResetHook {

    private final SpuJpaRepository spuJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public DatabaseResetHook(
            SpuJpaRepository spuJpaRepository,
            CategoryJpaRepository categoryJpaRepository) {
        this.spuJpaRepository = spuJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Before
    public void resetDb() {
        spuJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
    }
}