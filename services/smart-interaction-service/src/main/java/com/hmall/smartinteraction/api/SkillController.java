package com.hmall.smartinteraction.api;

import com.hmall.smartinteraction.api.dto.CreateSkillRequest;
import com.hmall.smartinteraction.api.dto.SkillDto;
import com.hmall.smartinteraction.api.dto.UpdateSkillRequest;
import com.hmall.smartinteraction.application.SkillApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/skills")
public class SkillController {

    private final SkillApplicationService skillService;

    public SkillController(SkillApplicationService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public List<SkillDto> list() {
        return skillService.list().stream().map(SkillDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateSkillRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name must not be blank"));
        }
        var skill = skillService.create(
            request.name(),
            request.description(),
            request.systemPrompt(),
            request.allowedTools() != null ? request.allowedTools() : List.of()
        );
        return ResponseEntity.ok(SkillDto.from(skill));
    }

    @GetMapping("/{skillId}")
    public ResponseEntity<SkillDto> get(@PathVariable Long skillId) {
        return skillService.findById(skillId)
            .map(skill -> ResponseEntity.ok(SkillDto.from(skill)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<?> update(@PathVariable Long skillId, @RequestBody UpdateSkillRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name must not be blank"));
        }
        return skillService.update(
            skillId,
            request.name(),
            request.description(),
            request.systemPrompt(),
            request.allowedTools() != null ? request.allowedTools() : List.of()
        )
            .map(skill -> ResponseEntity.ok(SkillDto.from(skill)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> delete(@PathVariable Long skillId) {
        if (skillService.delete(skillId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{skillId}/default")
    public ResponseEntity<SkillDto> setDefault(@PathVariable Long skillId) {
        return skillService.setDefault(skillId)
            .map(skill -> ResponseEntity.ok(SkillDto.from(skill)))
            .orElse(ResponseEntity.notFound().build());
    }
}
