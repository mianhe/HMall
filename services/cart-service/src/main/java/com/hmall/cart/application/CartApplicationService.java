package com.hmall.cart.application;

import com.hmall.cart.application.port.SkuInfo;
import com.hmall.cart.application.port.SkuQueryPort;
import com.hmall.cart.domain.Cart;
import com.hmall.cart.domain.CartItem;
import com.hmall.cart.domain.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final SkuQueryPort skuQueryPort;

    public CartApplicationService(CartRepository cartRepository, SkuQueryPort skuQueryPort) {
        this.cartRepository = cartRepository;
        this.skuQueryPort = skuQueryPort;
    }

    @Transactional
    public CartItem addItem(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new CartBadRequestException("数量必须 > 0");
        }
        if (!skuQueryPort.exists(skuId)) {
            throw new SkuNotFoundException(skuId);
        }
        Cart cart = getOrCreateCart(userId);
        cart.addItem(skuId, quantity);
        Cart saved = cartRepository.save(cart);
        return findItemBySkuId(saved, skuId);
    }

    @Transactional(readOnly = true)
    public List<CartItemView> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }
        Map<Long, SkuInfo> skuInfoMap = fetchSkuInfoMap(
            cart.getItems().stream().map(CartItem::getSkuId).toList()
        );
        return cart.getItems().stream()
            .map(item -> toCartItemView(item, skuInfoMap.get(item.getSkuId())))
            .toList();
    }

    @Transactional
    public CartItem updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity < 0) {
            throw new CartBadRequestException("数量不能为负数");
        }
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new CartNotFoundException(userId));
        CartItem item = cart.updateItemQuantity(cartItemId, quantity);
        cartRepository.save(cart);
        return item;
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
            .orElseThrow(() -> new CartNotFoundException(userId));
        cart.removeItem(cartItemId);
        cartRepository.save(cart);
    }

    @Transactional
    public void removeItems(Long userId, List<Long> cartItemIds) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return;
        }
        cart.removeItems(cartItemIds);
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CheckoutPreview checkoutPreview(Long userId, List<Long> cartItemIds) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new CartBadRequestException("购物车为空");
        }
        List<CartItem> selectedItems = cart.getItems().stream()
            .filter(i -> cartItemIds.contains(i.getCartItemId()))
            .toList();
        if (selectedItems.isEmpty()) {
            throw new CartBadRequestException("未选中任何购物车项");
        }

        Map<Long, SkuInfo> skuInfoMap = fetchSkuInfoMap(
            selectedItems.stream().map(CartItem::getSkuId).toList()
        );
        for (CartItem item : selectedItems) {
            SkuInfo info = skuInfoMap.get(item.getSkuId());
            if (info == null || !info.available()) {
                throw new CartBadRequestException("SKU " + item.getSkuId() + " 不可用");
            }
        }

        List<CheckoutPreview.Item> previewItems = selectedItems.stream().map(item -> {
            SkuInfo info = skuInfoMap.get(item.getSkuId());
            BigDecimal subtotal = info.price().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CheckoutPreview.Item(
                item.getCartItemId(), item.getSkuId(), info.name(),
                info.price(), item.getQuantity(), subtotal
            );
        }).toList();

        BigDecimal totalPrice = previewItems.stream()
            .map(CheckoutPreview.Item::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutPreview(previewItems, totalPrice);
    }

    private Map<Long, SkuInfo> fetchSkuInfoMap(List<Long> skuIds) {
        return skuQueryPort.queryByIds(skuIds).stream()
            .collect(Collectors.toMap(SkuInfo::skuId, Function.identity()));
    }

    private static CartItemView toCartItemView(CartItem item, SkuInfo info) {
        boolean available = info != null && info.available();
        return new CartItemView(
            item.getCartItemId(), item.getSkuId(), item.getQuantity(), item.getAddedAt(),
            info != null ? info.name() : null,
            info != null ? info.price() : null,
            info != null ? info.imageUrl() : null,
            available
        );
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
    }

    private CartItem findItemBySkuId(Cart cart, Long skuId) {
        return cart.getItems().stream()
            .filter(i -> i.getSkuId().equals(skuId))
            .findFirst()
            .orElseThrow();
    }
}
