package com.hmall.cart.domain;

import java.util.Optional;

/**
 * 购物车仓储：持久化与查询购物车（含其购物车项）。
 */
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByUserId(Long userId);
}
