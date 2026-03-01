package com.hmall.cart.api;

import com.hmall.cart.api.dto.AddCartItemRequest;
import com.hmall.cart.api.dto.CartItemDto;
import com.hmall.cart.api.dto.CheckoutPreviewDto;
import com.hmall.cart.api.dto.CheckoutRequest;
import com.hmall.cart.api.dto.DeleteCartItemsRequest;
import com.hmall.cart.api.dto.UpdateCartItemRequest;
import com.hmall.cart.application.CartApplicationService;
import com.hmall.cart.application.CartItemView;
import com.hmall.cart.application.CheckoutPreview;
import com.hmall.cart.application.AvailableServiceView;
import com.hmall.cart.domain.CartItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartApplicationService applicationService;

    public CartController(CartApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCart(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        List<CartItemView> views = applicationService.getCartItems(userId);
        List<CartItemDto> dtos = views.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemDto> addItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody AddCartItemRequest request) {
        requireUserId(userId);
        CartItem item = applicationService.addItem(userId, request.skuId(), request.relatedSkuId(), request.quantity());
        return ResponseEntity.ok(toBasicDto(item));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartItemDto> updateItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request) {
        requireUserId(userId);
        CartItem item = applicationService.updateItemQuantity(userId, cartItemId, request.quantity());
        if (item == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toBasicDto(item));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteItem(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long cartItemId) {
        requireUserId(userId);
        applicationService.removeItem(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> deleteItems(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody DeleteCartItemsRequest request) {
        requireUserId(userId);
        applicationService.removeItems(userId, request.cartItemIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout-preview")
    public ResponseEntity<CheckoutPreviewDto> checkoutPreview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CheckoutRequest request) {
        requireUserId(userId);
        CheckoutPreview preview = applicationService.checkoutPreview(userId, request.cartItemIds());
        return ResponseEntity.ok(toCheckoutDto(preview));
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
    }

    private CartItemDto toDto(CartItemView view) {
        return new CartItemDto(
            view.cartItemId(), view.skuId(), view.relatedSkuId(), view.quantity(), view.addedAt(),
            view.skuName(), view.skuPrice(), view.skuImageUrl(), view.available(),
            view.productType(), view.spuId(),
            toAvailableServiceDtos(view.availableServices())
        );
    }

    private CartItemDto toBasicDto(CartItem item) {
        return new CartItemDto(
            item.getCartItemId(), item.getSkuId(), item.getRelatedSkuId(), item.getQuantity(), item.getAddedAt(),
            null, null, null, null, null, null, null
        );
    }

    private CheckoutPreviewDto toCheckoutDto(CheckoutPreview preview) {
        List<CheckoutPreviewDto.Item> items = preview.items().stream()
            .map(i -> new CheckoutPreviewDto.Item(
                i.cartItemId(), i.skuId(), i.relatedSkuId(), i.productType(),
                i.skuName(), i.price(), i.quantity(), i.subtotal()
            ))
            .toList();
        List<CheckoutPreviewDto.Group> groups = preview.groups().stream()
            .map(g -> new CheckoutPreviewDto.Group(
                g.primaryCartItemId(),
                g.primarySkuId(),
                g.primarySkuName(),
                g.serviceItems().stream()
                    .map(i -> new CheckoutPreviewDto.Item(
                        i.cartItemId(), i.skuId(), i.relatedSkuId(), i.productType(),
                        i.skuName(), i.price(), i.quantity(), i.subtotal()
                    ))
                    .toList(),
                g.groupSubtotal()
            ))
            .toList();
        return new CheckoutPreviewDto(items, groups, preview.totalPrice());
    }

    private List<CartItemDto.AvailableServiceDto> toAvailableServiceDtos(List<AvailableServiceView> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }
        return services.stream()
            .map(s -> new CartItemDto.AvailableServiceDto(
                s.serviceSpuId(),
                s.name(),
                s.bindings().stream()
                    .map(b -> new CartItemDto.AvailableServiceSkuDto(b.bindingId(), b.serviceSkuId(), b.price()))
                    .toList()
            ))
            .toList();
    }
}
