package com.hmall.order.api;

import com.hmall.order.api.dto.OrderCreateDto;
import com.hmall.order.api.dto.OrderDto;
import com.hmall.order.api.dto.OrderListPageDto;
import com.hmall.order.api.dto.PaymentCompletedRequestDto;
import com.hmall.order.application.OrderApplicationService;
import com.hmall.order.application.OrderEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService applicationService;
    private final OrderEventService orderEventService;

    public OrderController(OrderApplicationService applicationService, OrderEventService orderEventService) {
        this.applicationService = applicationService;
        this.orderEventService = orderEventService;
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

    /** 内部接口：Payment 回调成功后通知订单支付完成，将订单置为 PAID 并触发履约。 */
    @PostMapping("/internal/payment-completed")
    public ResponseEntity<Void> paymentCompleted(@Valid @RequestBody PaymentCompletedRequestDto dto) {
        orderEventService.onPaymentCompleted(dto.orderId(), dto.paymentId());
        return ResponseEntity.ok().build();
    }
}

