package com.hmall.inventory.domain;

import java.util.List;

/**
 * 占用记录仓储。
 */
public interface ReservationRepository {

    List<Reservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    Reservation save(Reservation reservation);

    /** 是否存在该订单的已占用记录（用于幂等判断） */
    boolean existsByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
