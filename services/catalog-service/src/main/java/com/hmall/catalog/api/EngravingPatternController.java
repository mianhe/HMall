package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.EngravingPatternCreateDto;
import com.hmall.catalog.api.dto.EngravingPatternDto;
import com.hmall.catalog.api.dto.EngravingPatternUpdateDto;
import com.hmall.catalog.application.EngravingPatternApplicationService;
import com.hmall.catalog.application.EngravingPatternBadRequestException;
import com.hmall.catalog.domain.EngravingPattern;
import com.hmall.filestorage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EngravingPatternController {

    private final EngravingPatternApplicationService applicationService;
    private final FileStorageService fileStorageService;

    public EngravingPatternController(
            EngravingPatternApplicationService applicationService,
            @Autowired(required = false) FileStorageService fileStorageService) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/api/engraving-patterns")
    public ResponseEntity<EngravingPatternDto> create(@RequestBody EngravingPatternCreateDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new EngravingPatternBadRequestException("name 必填");
        }
        if (dto.imageUrl() == null || dto.imageUrl().isBlank()) {
            throw new EngravingPatternBadRequestException("imageUrl 必填");
        }
        EngravingPattern pattern = applicationService.create(
            dto.name(), dto.imageUrl(), dto.sortOrder(), dto.enabled());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(pattern));
    }

    @GetMapping("/api/engraving-patterns")
    public ResponseEntity<List<EngravingPatternDto>> list(
            @RequestParam(required = false) Boolean enabled) {
        List<EngravingPattern> patterns = applicationService.list(enabled);
        return ResponseEntity.ok(patterns.stream().map(this::toDto).toList());
    }

    @GetMapping("/api/engraving-patterns/{id}")
    public ResponseEntity<EngravingPatternDto> getById(@PathVariable Long id) {
        EngravingPattern pattern = applicationService.getById(id);
        return ResponseEntity.ok(toDto(pattern));
    }

    @PutMapping("/api/engraving-patterns/{id}")
    public ResponseEntity<EngravingPatternDto> update(
            @PathVariable Long id,
            @RequestBody EngravingPatternUpdateDto dto) {
        EngravingPattern pattern = applicationService.update(
            id, dto.name(), dto.imageUrl(), dto.sortOrder(), dto.enabled());
        return ResponseEntity.ok(toDto(pattern));
    }

    @DeleteMapping("/api/engraving-patterns/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EngravingPatternDto toDto(EngravingPattern p) {
        String imageUrl = p.getImageUrl();
        if (fileStorageService != null) {
            imageUrl = fileStorageService.toServeUrlIfMinio(imageUrl);
        }
        return new EngravingPatternDto(
            p.getId(), p.getName(), imageUrl, p.getSortOrder(), p.isEnabled());
    }
}
