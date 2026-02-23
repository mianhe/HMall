package com.hmall.smartinteraction.api;

import com.hmall.smartinteraction.api.dto.SettingsDto;
import com.hmall.smartinteraction.api.dto.UpdateSettingsRequest;
import com.hmall.smartinteraction.application.SettingsApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/settings")
public class SettingsController {

    private final SettingsApplicationService settingsService;

    public SettingsController(SettingsApplicationService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto get() {
        return SettingsDto.from(settingsService.getOrCreate());
    }

    @PutMapping
    public ResponseEntity<SettingsDto> update(@RequestBody UpdateSettingsRequest request) {
        var settings = settingsService.update(
            request.adminBasePrompt(),
            request.consumerBasePrompt()
        );
        return ResponseEntity.ok(SettingsDto.from(settings));
    }

    @PostMapping("/reset")
    public ResponseEntity<SettingsDto> reset() {
        var settings = settingsService.reset();
        return ResponseEntity.ok(SettingsDto.from(settings));
    }
}
