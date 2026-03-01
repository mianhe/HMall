package com.hmall.cart.infrastructure;

import com.hmall.cart.domain.Cart;
import com.hmall.cart.domain.CartItem;
import com.hmall.cart.domain.CartRepository;
import com.hmall.cart.infrastructure.persistence.CartEntity;
import com.hmall.cart.infrastructure.persistence.CartItemEntity;
import com.hmall.cart.infrastructure.persistence.CartJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository jpaRepository;

    public CartRepositoryImpl(CartJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = toEntity(cart);
        CartEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    private CartEntity toEntity(Cart domain) {
        CartEntity entity = new CartEntity();
        if (domain.getCartId() != null) {
            entity.setId(domain.getCartId());
        }
        entity.setUserId(domain.getUserId());
        entity.setUpdatedAt(domain.getUpdatedAt());

        List<CartItemEntity> itemEntities = new ArrayList<>();
        for (CartItem item : domain.getItems()) {
            CartItemEntity itemEntity = new CartItemEntity();
            if (item.getCartItemId() != null) {
                itemEntity.setId(item.getCartItemId());
            }
            itemEntity.setCart(entity);
            itemEntity.setSkuId(item.getSkuId());
            itemEntity.setRelatedSkuId(item.getRelatedSkuId());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setAddedAt(item.getAddedAt());
            itemEntities.add(itemEntity);
        }
        entity.setItems(itemEntities);
        return entity;
    }

    private Cart toDomain(CartEntity entity) {
        List<CartItem> items = entity.getItems().stream()
            .map(ie -> new CartItem(ie.getId(), ie.getSkuId(), ie.getRelatedSkuId(), ie.getQuantity(), ie.getAddedAt()))
            .toList();
        return new Cart(entity.getId(), entity.getUserId(), entity.getUpdatedAt(), items);
    }
}
