package com.hmall.activity.api;

import com.hmall.activity.api.dto.ActivityDto;
import com.hmall.activity.api.dto.EventMetadataDto;
import com.hmall.activity.api.dto.StatsDto;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.domain.ActivityStats;
import com.hmall.activity.domain.EventMetadataRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityApplicationService applicationService;

    public ActivityController(ActivityApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityDto>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "20") int limit) {
        if (orderId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<ActivityDto> list = applicationService.listByOrderId(orderId, limit).stream()
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
