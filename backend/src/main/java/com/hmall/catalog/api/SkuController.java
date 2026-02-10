package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.SkuCreateDto;
import com.hmall.catalog.api.dto.SkuDto;
import com.hmall.catalog.api.dto.SkuSpecValueDto;
import com.hmall.catalog.application.SkuApplicationService;
import com.hmall.catalog.domain.Sku;
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
@RequestMapping("/api/products/{spuId}/skus")
public class SkuController {

    private final SkuApplicationService applicationService;

    public SkuController(SkuApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<SkuDto> create(
            @PathVariable Long spuId,
            @RequestBody SkuCreateDto dto) {
        long priceCents = dto.priceCents() != null ? dto.priceCents() : 0L;
        Sku created = applicationService.createSku(
            spuId,
            dto.specOptionIds(),
            priceCents,
            dto.displayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toSkuDto(created));
    }

    @GetMapping
    public ResponseEntity<List<SkuDto>> list(@PathVariable Long spuId) {
        List<Sku> skus = applicationService.findBySpuId(spuId);
        return ResponseEntity.ok(skus.stream().map(this::toSkuDto).toList());
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<SkuDto> getDetail(@PathVariable Long spuId, @PathVariable Long skuId) {
        Sku sku = applicationService.getById(skuId);
        if (!sku.getSpuId().equals(spuId)) {
            throw new IllegalArgumentException("SKU 不属于该 SPU");
        }
        return ResponseEntity.ok(toSkuDto(sku));
    }

    private SkuDto toSkuDto(Sku sku) {
        List<SkuSpecValueDto> specValues = applicationService.resolveSpecValues(sku.getSpecOptionIds()).stream()
            .map(v -> new SkuSpecValueDto(v.dimensionName(), v.optionValue()))
            .toList();
        return new SkuDto(
            sku.getId(),
            sku.getSpuId(),
            sku.getPriceCents(),
            sku.getDisplayName(),
            specValues
        );
    }
}
