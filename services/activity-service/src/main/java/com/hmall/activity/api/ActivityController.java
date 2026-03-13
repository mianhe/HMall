package com.hmall.activity.api;

import com.hmall.activity.api.dto.ActivityDto;
import com.hmall.activity.api.dto.DailyStatsDto;
import com.hmall.activity.api.dto.EventMetadataDto;
import com.hmall.activity.api.dto.StatsDto;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.application.SeedDataGenerator;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.domain.EventMetadataRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityApplicationService applicationService;
    private final SeedDataGenerator seedDataGenerator;

    public ActivityController(ActivityApplicationService applicationService, SeedDataGenerator seedDataGenerator) {
        this.applicationService = applicationService;
        this.seedDataGenerator = seedDataGenerator;
    }

    /** 多维查询：orderId / userId / skuId / spuId 任一；多参数同时传入时按 orderId → userId → skuId → spuId 优先级取第一个。 */
    @GetMapping
    public ResponseEntity<List<ActivityDto>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long spuId,
            @RequestParam(defaultValue = "20") int limit) {
        List<BusinessActivity> activities;
        if (orderId != null) {
            activities = applicationService.listByOrderId(orderId, limit);
        } else if (userId != null) {
            activities = applicationService.listByUserId(userId, limit);
        } else if (skuId != null) {
            activities = applicationService.listByCorrelationKey("skuIds", skuId, limit);
        } else if (spuId != null) {
            activities = applicationService.listByCorrelationKey("spuIds", spuId, limit);
        } else {
            return ResponseEntity.ok(List.of());
        }
        List<ActivityDto> list = activities.stream()
            .map(ActivityDto::from)
            .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ActivityDto>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit) {
        List<ActivityDto> list = applicationService.listRecent(limit).stream()
            .map(ActivityDto::from)
            .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/event-metadata")
    public ResponseEntity<List<EventMetadataDto>> getEventMetadata() {
        List<EventMetadataDto> list = EventMetadataRegistry.all().stream()
            .map(EventMetadataDto::from)
            .toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAll() {
        applicationService.deleteAll();
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int ordersPerDay,
            @RequestParam(defaultValue = "0") int maxOrders) {
        var result = seedDataGenerator.generate(
            Math.min(days, 90),
            Math.min(ordersPerDay, 50),
            maxOrders <= 0 ? 0 : Math.min(maxOrders, 500)
        );
        return ResponseEntity.ok(Map.of(
            "ordersGenerated", result.ordersGenerated(),
            "eventsGenerated", result.eventsGenerated(),
            "timeRange", result.timeRange()
        ));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<List<DailyStatsDto>> getDailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period) {

        LocalDate fromDate;
        LocalDate toDate;
        LocalDate today = LocalDate.now();

        if (from != null && to != null) {
            fromDate = from;
            toDate = to;
        } else {
            toDate = today;
            fromDate = switch (period != null ? period : "last7") {
                case "today" -> today;
                case "last30" -> today.minusDays(29);
                default -> today.minusDays(6);
            };
        }

        List<DailyStatsDto> dailyStats = applicationService.getDailyStats(fromDate, toDate)
            .stream()
            .map(DailyStatsDto::from)
            .toList();
        return ResponseEntity.ok(dailyStats);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDto> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String period) {

        LocalDate fromDate;
        LocalDate toDate;
        LocalDate today = LocalDate.now();

        if (from != null && to != null) {
            fromDate = from;
            toDate = to;
        } else {
            toDate = today;
            fromDate = switch (period != null ? period : "today") {
                case "last7" -> today.minusDays(6);
                case "last30" -> today.minusDays(29);
                default -> today;
            };
        }

        ActivityStats stats = applicationService.getStats(fromDate, toDate);
        return ResponseEntity.ok(StatsDto.from(stats, fromDate, toDate));
    }
}
