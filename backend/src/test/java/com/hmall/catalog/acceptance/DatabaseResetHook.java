package com.hmall.catalog.acceptance;

import com.hmall.catalog.infrastructure.persistence.CategoryJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SpuJpaRepository;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;
import io.cucumber.java.Before;

/**
 * 每个场景执行前清空类别、商品与用户表，避免场景间数据干扰。由 AcceptanceTestConfig 注册为 Bean。
 */
public class DatabaseResetHook {

    private final SpuJpaRepository spuJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public DatabaseResetHook(
            SpuJpaRepository spuJpaRepository,
            CategoryJpaRepository categoryJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.spuJpaRepository = spuJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Before
    public void resetDb() {
        spuJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }
}