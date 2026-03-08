package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.SkuDto;
import com.hmall.catalog.api.dto.SkuSpecValueDto;
import com.hmall.catalog.application.SkuApplicationService;
import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.Spu;
import com.hmall.catalog.domain.SpuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SKU 按 ID 查询接口，供 Order 等下游微服务调用。
 * 与 SkuController（按 SPU 管理）分离，避免 Order 需知 spuId。
 */
@RestController
@RequestMapping("/api/skus")
public class SkuLookupController {

    private final SkuApplicationService applicationService;
    private final SpuRepository spuRepository;

    public SkuLookupController(SkuApplicationService applicationService, SpuRepository spuRepository) {
        this.applicationService = applicationService;
        this.spuRepository = spuRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkuDto> getById(@PathVariable Long id) {
        Sku sku = applicationService.getById(id);
        Spu spu = spuRepository.findById(sku.getSpuId()).orElse(null);
        String spuName = spu != null ? spu.getName() : "";
        String productType = spu != null ? spu.getProductType() : "PHYSICAL";
        List<SkuSpecValueDto> specValues = applicationService.resolveSpecValues(sku.getSpecOptionIds()).stream()
            .map(v -> new SkuSpecValueDto(v.dimensionName(), v.optionValue()))
            .toList();
        return ResponseEntity.ok(new SkuDto(sku.getId(), sku.getSpuId(), sku.getPriceCents(),
            sku.getDisplayName(), spuName, productType, specValues));
    }
}
