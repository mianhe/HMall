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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final SkuQueryPort skuQueryPort;

    public CartApplicationService(CartRepository cartRepository, SkuQueryPort skuQueryPort) {
        this.cartRepository = cartRepository;
        this.skuQueryPort = skuQueryPort;
    }

    @Transactional
    public CartItem addItem(Long userId, Long skuId, Long relatedSkuId, int quantity) {
        if (quantity <= 0) {
            throw new CartBadRequestException("数量必须 > 0");
        }
        SkuInfo skuInfo = skuQueryPort.queryById(skuId).orElse(null);
        if (skuInfo == null) {
            throw new SkuNotFoundException(skuId);
        }
        String productType = skuInfo.productType() != null ? skuInfo.productType() : "PHYSICAL";
        if ("SERVICE".equals(productType) && relatedSkuId == null) {
            throw new CartBadRequestException("服务商品必须关联实体 sku");
        }
        Cart cart = getOrCreateCart(userId);
        cart.addItem(skuId, relatedSkuId, quantity);
        Cart saved = cartRepository.save(cart);
        return findItemBySkuId(saved, skuId, relatedSkuId);
    }

    @Transactional(readOnly = true)
    public List<CartItemView> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }
        List<CartItem> allItems = cart.getItems();
        List<Long> skuIdsToQuery = Stream.concat(
                allItems.stream().map(CartItem::getSkuId),
                allItems.stream().map(CartItem::getRelatedSkuId).filter(Objects::nonNull)
            )
            .distinct()
            .toList();
        Map<Long, SkuInfo> skuInfoMap = fetchSkuInfoMap(skuIdsToQuery);
        List<CartItem> visibleItems = filterOutInvisibleItems(allItems, skuInfoMap);
        Map<ServicePriceKey, BigDecimal> serviceBindingPriceMap = buildServiceBindingPriceMap(visibleItems, skuInfoMap);
        Map<Long, List<AvailableServiceView>> availableServicesMap = buildAvailableServicesMap(cart, skuInfoMap);
        return visibleItems.stream()
            .map(item -> toCartItemView(
                item,
                skuInfoMap.get(item.getSkuId()),
                availableServicesMap.getOrDefault(item.getCartItemId(), List.of()),
                resolveCartDisplayUnitPrice(item, skuInfoMap.get(item.getSkuId()), serviceBindingPriceMap)
            ))
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

        List<Long> skuIdsToQuery = Stream.concat(
                selectedItems.stream().map(CartItem::getSkuId),
                selectedItems.stream().map(CartItem::getRelatedSkuId).filter(Objects::nonNull)
            )
            .distinct()
            .toList();
        Map<Long, SkuInfo> skuInfoMap = fetchSkuInfoMap(skuIdsToQuery);
        selectedItems = filterOutInvisibleItems(selectedItems, skuInfoMap);
        if (selectedItems.isEmpty()) {
            throw new CartBadRequestException("未选中任何有效购物车项");
        }
        for (CartItem item : selectedItems) {
            SkuInfo info = skuInfoMap.get(item.getSkuId());
            if (info == null || !info.available()) {
                throw new CartBadRequestException("SKU " + item.getSkuId() + " 不可用");
            }
        }

        Map<ServicePriceKey, BigDecimal> serviceBindingPriceMap = buildServiceBindingPriceMap(selectedItems, skuInfoMap);
        List<CheckoutPreview.Item> previewItems = selectedItems.stream().map(item -> {
            SkuInfo info = skuInfoMap.get(item.getSkuId());
            BigDecimal price = resolveCheckoutUnitPrice(item, info, serviceBindingPriceMap);
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CheckoutPreview.Item(
                item.getCartItemId(), item.getSkuId(), item.getRelatedSkuId(), info.productType(), info.name(),
                price, item.getQuantity(), subtotal
            );
        }).toList();

        List<CheckoutPreview.Group> groups = buildCheckoutGroups(previewItems);

        BigDecimal totalPrice = previewItems.stream()
            .map(CheckoutPreview.Item::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutPreview(previewItems, groups, totalPrice);
    }

    private Map<Long, SkuInfo> fetchSkuInfoMap(List<Long> skuIds) {
        List<Long> distinctIds = skuIds.stream().distinct().toList();
        return skuQueryPort.queryByIds(distinctIds).stream()
            .collect(Collectors.toMap(SkuInfo::skuId, Function.identity()));
    }

    private Map<Long, List<AvailableServiceView>> buildAvailableServicesMap(Cart cart, Map<Long, SkuInfo> skuInfoMap) {
        Map<Long, List<AvailableServiceView>> result = new java.util.HashMap<>();
        for (CartItem item : cart.getItems()) {
            SkuInfo info = skuInfoMap.get(item.getSkuId());
            if (info == null || !"PHYSICAL".equals(info.productType())) {
                result.put(item.getCartItemId(), List.of());
                continue;
            }
            List<AvailableServiceView> services = skuQueryPort.queryAvailableServices(info.spuId()).stream()
                .map(s -> new AvailableServiceView(
                    s.serviceSpuId(),
                    s.name(),
                    s.bindings().stream()
                        .map(b -> new AvailableServiceView.AvailableServiceSkuView(
                            b.bindingId(), b.serviceSkuId(), b.price()
                        ))
                        .toList()
                ))
                .toList();
            result.put(item.getCartItemId(), services);
        }
        return result;
    }

    private static CartItemView toCartItemView(
        CartItem item,
        SkuInfo info,
        List<AvailableServiceView> services,
        BigDecimal resolvedPrice
    ) {
        boolean available = info != null && info.available();
        return new CartItemView(
            item.getCartItemId(), item.getSkuId(), item.getRelatedSkuId(), item.getQuantity(), item.getAddedAt(),
            info != null ? info.name() : null,
            resolvedPrice,
            info != null ? info.imageUrl() : null,
            available,
            info != null ? info.productType() : null,
            info != null ? info.spuId() : null,
            services
        );
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
    }

    private CartItem findItemBySkuId(Cart cart, Long skuId, Long relatedSkuId) {
        return cart.getItems().stream()
            .filter(i -> i.getSkuId().equals(skuId) && java.util.Objects.equals(i.getRelatedSkuId(), relatedSkuId))
            .findFirst()
            .orElseThrow();
    }

    private static List<CheckoutPreview.Group> buildCheckoutGroups(List<CheckoutPreview.Item> items) {
        Map<Long, CheckoutPreview.Item> physicalByCartItemId = items.stream()
            .filter(i -> "PHYSICAL".equals(i.productType()))
            .collect(Collectors.toMap(CheckoutPreview.Item::cartItemId, Function.identity()));

        Map<Long, List<CheckoutPreview.Item>> servicesByRelatedCartItemId = items.stream()
            .filter(i -> "SERVICE".equals(i.productType()) && i.relatedSkuId() != null)
            .collect(Collectors.groupingBy(CheckoutPreview.Item::relatedSkuId));

        return physicalByCartItemId.values().stream()
            .map(p -> {
                List<CheckoutPreview.Item> services = servicesByRelatedCartItemId.getOrDefault(p.skuId(), List.of());
                BigDecimal groupSubtotal = services.stream()
                    .map(CheckoutPreview.Item::subtotal)
                    .reduce(p.subtotal(), BigDecimal::add);
                return new CheckoutPreview.Group(
                    p.cartItemId(),
                    p.skuId(),
                    p.skuName(),
                    services,
                    groupSubtotal
                );
            })
            .toList();
    }

    private Map<ServicePriceKey, BigDecimal> buildServiceBindingPriceMap(List<CartItem> selectedItems, Map<Long, SkuInfo> skuInfoMap) {
        Map<Long, List<SkuQueryPort.AvailableService>> servicesByTargetSpuId = new java.util.HashMap<>();
        Map<ServicePriceKey, BigDecimal> result = new java.util.HashMap<>();
        for (CartItem item : selectedItems) {
            if (!isServiceItem(item, skuInfoMap)) {
                continue;
            }
            SkuInfo relatedSkuInfo = skuInfoMap.get(item.getRelatedSkuId());
            if (relatedSkuInfo == null || relatedSkuInfo.spuId() == null) {
                continue;
            }
            List<SkuQueryPort.AvailableService> availableServices = servicesByTargetSpuId.computeIfAbsent(
                relatedSkuInfo.spuId(),
                skuQueryPort::queryAvailableServices
            );
            availableServices.stream()
                .flatMap(service -> service.bindings().stream())
                .filter(binding -> binding.serviceSkuId().equals(item.getSkuId()))
                .findFirst()
                .ifPresent(binding -> result.put(new ServicePriceKey(item.getSkuId(), item.getRelatedSkuId()), binding.price()));
        }
        return result;
    }

    private static BigDecimal resolveCheckoutUnitPrice(
        CartItem item,
        SkuInfo skuInfo,
        Map<ServicePriceKey, BigDecimal> serviceBindingPriceMap
    ) {
        if (!"SERVICE".equals(skuInfo.productType()) || item.getRelatedSkuId() == null) {
            return skuInfo.price();
        }
        return serviceBindingPriceMap.getOrDefault(
            new ServicePriceKey(item.getSkuId(), item.getRelatedSkuId()),
            skuInfo.price()
        );
    }

    private static BigDecimal resolveCartDisplayUnitPrice(
        CartItem item,
        SkuInfo skuInfo,
        Map<ServicePriceKey, BigDecimal> serviceBindingPriceMap
    ) {
        if (skuInfo == null) {
            return null;
        }
        if (!"SERVICE".equals(skuInfo.productType()) || item.getRelatedSkuId() == null) {
            return skuInfo.price();
        }
        return serviceBindingPriceMap.getOrDefault(
            new ServicePriceKey(item.getSkuId(), item.getRelatedSkuId()),
            skuInfo.price()
        );
    }

    private static boolean isServiceItem(CartItem item, Map<Long, SkuInfo> skuInfoMap) {
        SkuInfo skuInfo = skuInfoMap.get(item.getSkuId());
        return skuInfo != null && "SERVICE".equals(skuInfo.productType()) && item.getRelatedSkuId() != null;
    }

    private static List<CartItem> filterOutInvisibleItems(List<CartItem> items, Map<Long, SkuInfo> skuInfoMap) {
        java.util.Set<Long> primarySkuIds = items.stream()
            .filter(i -> {
                SkuInfo info = skuInfoMap.get(i.getSkuId());
                return info != null && !"SERVICE".equals(info.productType()) && i.getRelatedSkuId() == null;
            })
            .map(CartItem::getSkuId)
            .collect(Collectors.toSet());
        return items.stream()
            .filter(i -> {
                SkuInfo info = skuInfoMap.get(i.getSkuId());
                if (info == null) {
                    return false;
                }
                if ("SERVICE".equals(info.productType())) {
                    return i.getRelatedSkuId() != null && primarySkuIds.contains(i.getRelatedSkuId());
                }
                return i.getRelatedSkuId() == null;
            })
            .toList();
    }

    private record ServicePriceKey(Long serviceSkuId, Long relatedSkuId) {}
}
