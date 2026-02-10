package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.SpecDimensionCreateDto;
import com.hmall.catalog.api.dto.SpecDimensionDto;
import com.hmall.catalog.api.dto.SpecDimensionWithOptionsDto;
import com.hmall.catalog.api.dto.SpecOptionCreateDto;
import com.hmall.catalog.api.dto.SpecOptionDto;
import com.hmall.catalog.application.SpecDimensionApplicationService;
import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecOption;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{spuId}/dimensions")
public class SpecDimensionController {

    private final SpecDimensionApplicationService applicationService;

    public SpecDimensionController(SpecDimensionApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<SpecDimensionDto> create(
            @PathVariable Long spuId,
            @RequestBody SpecDimensionCreateDto dto) {
        boolean required = dto.required() != null && dto.required();
        boolean affectsAppearance = dto.affectsAppearance() != null && dto.affectsAppearance();
        SpecDimension created = applicationService.createDimension(
            spuId,
            dto.name(),
            required,
            dto.sortOrder(),
            affectsAppearance
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<SpecDimensionWithOptionsDto>> list(@PathVariable Long spuId) {
        var list = applicationService.listDimensionsWithOptions(spuId);
        List<SpecDimensionWithOptionsDto> body = list.stream()
            .map(row -> new SpecDimensionWithOptionsDto(
                row.dimension().getId(),
                row.dimension().getSpuId(),
                row.dimension().getName(),
                row.dimension().isRequired(),
                row.dimension().getSortOrder(),
                row.dimension().isAffectsAppearance(),
                row.options().stream()
                    .map(this::toOptionDto)
                    .toList()
            ))
            .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{dimensionId}/options")
    public ResponseEntity<SpecOptionDto> createOption(
            @PathVariable Long spuId,
            @PathVariable Long dimensionId,
            @RequestBody SpecOptionCreateDto dto) {
        SpecOption created = applicationService.createOption(
            spuId,
            dimensionId,
            dto.optionValue(),
            dto.sortOrder(),
            dto.image()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toOptionDto(created));
    }

    private static SpecDimensionDto toDto(SpecDimension d) {
        return new SpecDimensionDto(
            d.getId(),
            d.getSpuId(),
            d.getName(),
            d.isRequired(),
            d.getSortOrder(),
            d.isAffectsAppearance()
        );
    }

    private SpecOptionDto toOptionDto(SpecOption o) {
        return new SpecOptionDto(
            o.getId(),
            o.getSpecDimensionId(),
            o.getOptionValue(),
            o.getSortOrder(),
            o.getImage()
        );
    }
}
