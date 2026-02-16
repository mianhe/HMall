package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.OptionImageCreateDto;
import com.hmall.catalog.api.dto.OptionImageDto;
import com.hmall.catalog.api.dto.SpecDimensionCreateDto;
import com.hmall.catalog.api.dto.SpecDimensionDto;
import com.hmall.catalog.api.dto.SpecDimensionWithOptionsDto;
import com.hmall.catalog.api.dto.SpecOptionCreateDto;
import com.hmall.catalog.api.dto.SpecOptionDto;
import com.hmall.catalog.application.SpecDimensionApplicationService;
import com.hmall.catalog.domain.ProductImage;
import com.hmall.catalog.domain.SpecDimension;
import com.hmall.catalog.domain.SpecOption;
import com.hmall.filestorage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final FileStorageService fileStorageService;

    public SpecDimensionController(
            SpecDimensionApplicationService applicationService,
            @Autowired(required = false) FileStorageService fileStorageService) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<SpecDimensionDto> create(
            @PathVariable Long spuId,
            @RequestBody SpecDimensionCreateDto dto) {
        boolean required = dto.required() != null && dto.required();
        SpecDimension created = applicationService.createDimension(
            spuId,
            dto.name(),
            required,
            dto.sortOrder()
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
                row.optionsWithImages().stream()
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
            dto.sortOrder()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toOptionDto(created));
    }

    @DeleteMapping("/{dimensionId}/options/{optionId}")
    public ResponseEntity<Void> deleteOption(
            @PathVariable Long spuId,
            @PathVariable Long dimensionId,
            @PathVariable Long optionId) {
        applicationService.deleteOption(spuId, dimensionId, optionId);
        return ResponseEntity.noContent().build();
    }

    // ---------- 展示图端点 ----------

    @PostMapping("/{dimensionId}/options/{optionId}/images")
    public ResponseEntity<OptionImageDto> addOptionImage(
            @PathVariable Long spuId,
            @PathVariable Long dimensionId,
            @PathVariable Long optionId,
            @RequestBody OptionImageCreateDto dto) {
        ProductImage created = applicationService.addOptionImage(
            spuId, dimensionId, optionId,
            dto.imageUrl(), dto.sortOrder()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toOptionImageDto(created));
    }

    @GetMapping("/{dimensionId}/options/{optionId}/images")
    public ResponseEntity<List<OptionImageDto>> listOptionImages(
            @PathVariable Long spuId,
            @PathVariable Long dimensionId,
            @PathVariable Long optionId) {
        List<ProductImage> images = applicationService.listOptionImages(spuId, dimensionId, optionId);
        return ResponseEntity.ok(images.stream().map(this::toOptionImageDto).toList());
    }

    @DeleteMapping("/{dimensionId}/options/{optionId}/images/{imageId}")
    public ResponseEntity<Void> deleteOptionImage(
            @PathVariable Long spuId,
            @PathVariable Long dimensionId,
            @PathVariable Long optionId,
            @PathVariable Long imageId) {
        applicationService.deleteOptionImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // ---------- 映射 ----------

    private static SpecDimensionDto toDto(SpecDimension d) {
        return new SpecDimensionDto(
            d.getId(),
            d.getSpuId(),
            d.getName(),
            d.isRequired(),
            d.getSortOrder()
        );
    }

    private SpecOptionDto toOptionDto(SpecDimensionApplicationService.OptionWithImages owi) {
        return new SpecOptionDto(
            owi.option().getId(),
            owi.option().getSpecDimensionId(),
            owi.option().getOptionValue(),
            owi.option().getSortOrder(),
            owi.images().stream().map(this::toOptionImageDto).toList()
        );
    }

    private SpecOptionDto toOptionDto(SpecOption o) {
        return new SpecOptionDto(
            o.getId(),
            o.getSpecDimensionId(),
            o.getOptionValue(),
            o.getSortOrder(),
            List.of()
        );
    }

    /** 选项级展示图响应（与 OptionImageDto 形状一致） */
    private OptionImageDto toOptionImageDto(ProductImage img) {
        String imageUrl = img.getImageUrl();
        if (fileStorageService != null) {
            imageUrl = fileStorageService.toServeUrlIfMinio(imageUrl);
        }
        return new OptionImageDto(
            img.getId(),
            img.getSpecOptionId(),
            imageUrl,
            img.getSortOrder()
        );
    }
}
