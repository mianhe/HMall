package com.hmall.user.api;

import com.hmall.user.api.dto.UserCreateDto;
import com.hmall.user.api.dto.UserDto;
import com.hmall.user.api.dto.UserSegmentsDto;
import com.hmall.user.api.dto.*;
import com.hmall.user.application.ReasonStat;
import com.hmall.user.application.SegmentRulePreviewResult;
import com.hmall.user.application.UserApplicationService;
import com.hmall.user.domain.SegmentCondition;
import com.hmall.user.domain.SegmentRule;
import com.hmall.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService applicationService;

    public UserController(UserApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserCreateDto dto) {
        User created = applicationService.create(dto.username(), dto.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping
    public List<UserDto> list() {
        return applicationService.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        User user = applicationService.getById(id);
        return ResponseEntity.ok(toDto(user));
    }

    @GetMapping("/{id}/segments")
    public ResponseEntity<UserSegmentsDto> getSegments(@PathVariable Long id) {
        User user = applicationService.getById(id);
        return ResponseEntity.ok(new UserSegmentsDto(user.getId(), user.getLevel(), user.getTags()));
    }

    @PutMapping("/{id}/level")
    public ResponseEntity<UserDto> updateLevel(@PathVariable Long id, @RequestBody UpdateUserLevelRequestDto dto) {
        User user = applicationService.updateLevel(id, dto.level());
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<UserDto> updateTags(@PathVariable Long id, @RequestBody UpdateUserTagsRequestDto dto) {
        User user = applicationService.replaceTags(id, dto.tags());
        return ResponseEntity.ok(toDto(user));
    }

    @PostMapping("/segment-rules")
    public ResponseEntity<SegmentRuleDto> createSegmentRule(@RequestBody CreateSegmentRuleRequestDto dto) {
        if (dto.conditions() == null) {
            throw new com.hmall.user.application.UserBadRequestException("圈选条件不能为空");
        }
        SegmentRule rule = applicationService.createSegmentRule(dto.name(), toDomain(dto.conditions()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(rule));
    }

    @GetMapping("/segment-rules")
    public List<SegmentRuleDto> listSegmentRules() {
        return applicationService.listSegmentRules().stream().map(this::toDto).toList();
    }

    @GetMapping("/segment-rules/{ruleId}")
    public ResponseEntity<SegmentRuleDto> getSegmentRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(toDto(applicationService.getSegmentRule(ruleId)));
    }

    @PostMapping("/segment-rules/{ruleId}/preview")
    public ResponseEntity<SegmentRulePreviewResponseDto> previewSegmentRule(
        @PathVariable Long ruleId,
        @RequestBody(required = false) SegmentRulePreviewRequestDto dto
    ) {
        Integer sampleSize = dto == null ? null : dto.sampleSize();
        SegmentRulePreviewResult result = applicationService.previewSegmentRule(ruleId, sampleSize);
        return ResponseEntity.ok(toDto(result));
    }

    @PostMapping("/segment-rules/{ruleId}/activate")
    public ResponseEntity<SegmentRuleDto> activateSegmentRule(@PathVariable Long ruleId) {
        SegmentRule activated = applicationService.activateSegmentRule(ruleId);
        return ResponseEntity.ok(toDto(activated));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getLevel(), user.getTags());
    }

    private SegmentCondition toDomain(SegmentConditionDto dto) {
        return new SegmentCondition(dto.levelsIn(), dto.tagsAny(), dto.tagsAll(), dto.excludeTags());
    }

    private SegmentRuleDto toDto(SegmentRule rule) {
        SegmentCondition conditions = rule.getConditions();
        SegmentConditionDto dto = new SegmentConditionDto(
            conditions.levelsIn(),
            conditions.tagsAny(),
            conditions.tagsAll(),
            conditions.excludeTags()
        );
        return new SegmentRuleDto(rule.getId(), rule.getName(), rule.getStatus(), dto, rule.getLastPreviewCount(), rule.getUpdatedAt());
    }

    private SegmentRulePreviewResponseDto toDto(SegmentRulePreviewResult result) {
        List<ReasonStatDto> reasonStats = result.reasonStats().stream()
            .map(this::toDto)
            .toList();
        return new SegmentRulePreviewResponseDto(result.ruleId(), result.hitCount(), result.sampleUserIds(), reasonStats);
    }

    private ReasonStatDto toDto(ReasonStat stat) {
        return new ReasonStatDto(stat.reason(), stat.count());
    }
}
