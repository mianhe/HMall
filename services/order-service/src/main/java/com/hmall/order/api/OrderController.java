package com.hmall.order.api;

import com.hmall.order.api.dto.OrderCreateDto;
import com.hmall.order.api.dto.OrderDto;
import com.hmall.order.api.dto.OrderListPageDto;
import com.hmall.order.api.dto.PurchasableServiceDto;
import com.hmall.order.application.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService applicationService;

    public OrderController(OrderApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getById(@PathVariable Long orderId) {
        OrderDto order = applicationService.getById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<OrderListPageDto> list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OrderListPageDto result = applicationService.listByUserId(userId, page, size);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderCreateDto dto) {
        OrderDto created = applicationService.placeOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long orderId) {
        applicationService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{orderId}/purchasable-services")
    public ResponseEntity<List<PurchasableServiceDto>> getPurchasableServices(@PathVariable Long orderId) {
        List<PurchasableServiceDto> services = applicationService.getPurchasableServices(orderId);
        return ResponseEntity.ok(services);
    }
}

