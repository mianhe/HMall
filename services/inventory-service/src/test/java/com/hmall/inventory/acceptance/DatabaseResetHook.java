package com.hmall.inventory.acceptance;

import com.hmall.inventory.infrastructure.persistence.ReservationJpaRepository;
import com.hmall.inventory.infrastructure.persistence.SkuStockJpaRepository;
import io.cucumber.java.Before;
import org.springframework.stereotype.Component;

/**
 * 每个场景执行前清空库存与占用表，保证场景隔离。
 */
public class DatabaseResetHook {

    private final SkuStockJpaRepository skuStockJpaRepository;
    private final ReservationJpaRepository reservationJpaRepository;
    private final EventCapture eventCapture;

    public DatabaseResetHook(SkuStockJpaRepository skuStockJpaRepository,
                             ReservationJpaRepository reservationJpaRepository,
                             EventCapture eventCapture) {
        this.skuStockJpaRepository = skuStockJpaRepository;
        this.reservationJpaRepository = reservationJpaRepository;
        this.eventCapture = eventCapture;
    }

    @Before
    public void reset() {
        reservationJpaRepository.deleteAll();
        skuStockJpaRepository.deleteAll();
        eventCapture.clear();
    }
}
