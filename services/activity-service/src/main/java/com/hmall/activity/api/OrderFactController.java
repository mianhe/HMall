package com.hmall.activity.api;

import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.domain.OrderFact;
import com.hmall.activity.domain.OrderFactDailyStats;
import com.hmall.activity.domain.OrderFactStats;
import com.hmall.activity.domain.OrderItemFact;
import com.hmall.activity.domain.ProductRanking;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-facts")
public class OrderFactController {

    private final ActivityApplicationService applicationService;

    public OrderFactController(ActivityApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderFact> getByOrderId(@PathVariable Long orderId) {
        return applicationService.getOrderFact(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItemFact>> getItemsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(applicationService.getOrderItemFacts(orderId));
    }

    @GetMapping("/stats")
    public ResponseEntity<OrderFactStats> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period) {
        LocalDate[] range = resolveDateRange(from, to, period);
        return ResponseEntity.ok(applicationService.getOrderFactStats(range[0], range[1]));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<List<OrderFactDailyStats>> getDailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period) {
        LocalDate[] range = resolveDateRange(from, to, period);
        return ResponseEntity.ok(applicationService.getOrderFactDailyStats(range[0], range[1]));
    }

    @GetMapping("/product-ranking")
    public ResponseEntity<ProductRanking> getProductRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String rankBy,
            @RequestParam(required = false) Boolean hasEngraving,
            @RequestParam(required = false) Boolean hasWarranty,
            @RequestParam(required = false) String groupBy,
            @RequestParam(defaultValue = "20") int limit) {
        LocalDate[] range = resolveDateRange(from, to, period);
        return ResponseEntity.ok(applicationService.getProductRanking(
            range[0], range[1], rankBy, hasEngraving, hasWarranty, groupBy, limit));
    }

    @GetMapping
    public ResponseEntity<List<OrderFact>> listFacts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Boolean hasEngraving,
            @RequestParam(required = false) Boolean hasWarranty,
            @RequestParam(required = false) String currentStage,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        LocalDate[] range = resolveDateRange(from, to, period);
        return ResponseEntity.ok(applicationService.listOrderFacts(
            range[0], range[1], hasEngraving, hasWarranty, currentStage, userId, limit));
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuild() {
        int count = applicationService.rebuildOrderFacts();
        return ResponseEntity.ok(Map.of("rebuiltOrders", count));
    }

    private LocalDate[] resolveDateRange(LocalDate from, LocalDate to, String period) {
        if (from != null && to != null) {
            return new LocalDate[]{from, to};
        }
        LocalDate today = LocalDate.now();
        LocalDate resolvedTo = today;
        LocalDate resolvedFrom = switch (period != null ? period : "last7") {
            case "today" -> today;
            case "last30" -> today.minusDays(29);
            default -> today.minusDays(6);
        };
        return new LocalDate[]{resolvedFrom, resolvedTo};
    }
}
