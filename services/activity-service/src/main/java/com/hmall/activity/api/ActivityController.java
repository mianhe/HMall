package com.hmall.activity.api;

import com.hmall.activity.api.dto.ActivityDto;
import com.hmall.activity.api.dto.DailyStatsDto;
import com.hmall.activity.api.dto.EventMetadataDto;
import com.hmall.activity.api.dto.StatsDto;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.application.SeedDataGenerator;
import com.hmall.activity.application.SeedRequest;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.domain.EventMetadataRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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
    public ResponseEntity<Map<String, Object>> seed(@RequestBody(required = false) SeedRequest body,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer ordersPerDay,
            @RequestParam(required = false) Integer maxOrders) {

        SeedRequest request;
        if (body != null) {
            request = body;
        } else {
            int d = days != null ? Math.min(days, 365) : 30;
            int opd = ordersPerDay != null ? Math.min(ordersPerDay, 50) : 5;
            int mo = maxOrders != null && maxOrders > 0 ? Math.min(maxOrders, 5000) : 0;
            LocalDate today = LocalDate.now();
            request = new SeedRequest(today.minusDays(d - 1), today, opd, mo, null,
                null, null, null, null, null, null);
        }

        var result = seedDataGenerator.generate(request);
        int rebuiltOrders = applicationService.rebuildOrderFacts();
        Map<String, Object> response = new HashMap<>();
        response.put("ordersGenerated", result.ordersGenerated());
        response.put("eventsGenerated", result.eventsGenerated());
        response.put("timeRange", result.timeRange());
        response.put("batchTag", result.batchTag());
        response.put("orderFactsRebuilt", rebuiltOrders);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seed/batches")
    public ResponseEntity<List<Map<String, Object>>> seedBatches() {
        List<Object[]> summaries = applicationService.getSeedBatchSummaries();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : summaries) {
            Map<String, Object> item = new HashMap<>();
            item.put("batchTag", row[0]);
            item.put("eventCount", ((Number) row[1]).longValue());
            item.put("orderCount", ((Number) row[2]).longValue());
            item.put("minOccurredAt", row[3]);
            item.put("maxOccurredAt", row[4]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/seed")
    public ResponseEntity<Map<String, Object>> deleteSeedData(
            @RequestParam(required = false) String batchTag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (batchTag != null && !batchTag.isBlank()) {
            applicationService.deleteSeedByBatch(batchTag);
            return ResponseEntity.ok(Map.of("deleted", true, "scope", "batchTag=" + batchTag));
        }
        if (from != null && to != null) {
            ZoneId zone = ZoneId.systemDefault();
            Instant fromInstant = from.atStartOfDay(zone).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(zone).toInstant();
            applicationService.deleteSeedDataInRange(fromInstant, toInstant);
            return ResponseEntity.ok(Map.of("deleted", true, "scope", "range=" + from + "~" + to));
        }
        applicationService.deleteAllSeedData();
        return ResponseEntity.ok(Map.of("deleted", true, "scope", "allSeedData"));
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
