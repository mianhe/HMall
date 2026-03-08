package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ProductCreateDto;
import com.hmall.catalog.api.dto.ProductDto;
import com.hmall.catalog.api.dto.ProductImageDto;
import com.hmall.catalog.api.dto.ProductUpdateDto;
import com.hmall.catalog.api.dto.SkuDto;
import com.hmall.catalog.api.dto.SkuSpecValueDto;
import com.hmall.catalog.application.SkuApplicationService;
import com.hmall.catalog.application.SpecDimensionApplicationService;
import com.hmall.catalog.application.SpuApplicationService;
import com.hmall.catalog.domain.ProductImage;
import com.hmall.catalog.domain.Sku;
import com.hmall.catalog.domain.SkuRepository;
import com.hmall.catalog.domain.Spu;
import com.hmall.filestorage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final SpuApplicationService applicationService;
    private final SpecDimensionApplicationService specDimensionApplicationService;
    private final SkuApplicationService skuApplicationService;
    private final SkuRepository skuRepository;
    private final FileStorageService fileStorageService;

    public ProductController(
            SpuApplicationService applicationService,
            SpecDimensionApplicationService specDimensionApplicationService,
            SkuApplicationService skuApplicationService,
            SkuRepository skuRepository,
            @Autowired(required = false) FileStorageService fileStorageService) {
        this.applicationService = applicationService;
        this.specDimensionApplicationService = specDimensionApplicationService;
        this.skuApplicationService = skuApplicationService;
        this.skuRepository = skuRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody ProductCreateDto dto) {
        Spu created = applicationService.create(
            dto.categoryId(),
            dto.name(),
            dto.description(),
            dto.productType(),
            dto.serviceKind()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDto(created));
    }

    @GetMapping
    public List<ProductDto> list(
            @RequestParam Long categoryId,
            @RequestParam(required = false) String include) {
        List<Spu> spus = applicationService.listByCategoryId(categoryId);
        return toDtoList(spus, includeSkus(include));
    }

    @GetMapping("/search")
    public List<ProductDto> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String include) {
        List<Spu> spus = applicationService.searchByName(keyword);
        return toDtoList(spus, includeSkus(include));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        Spu spu = applicationService.getById(id);
        return ResponseEntity.ok(toDto(spu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody ProductUpdateDto dto) {
        Spu updated = applicationService.update(id, dto.name(), dto.description(), dto.categoryId());
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean includeSkus(String include) {
        return "skus".equalsIgnoreCase(include);
    }

    private List<ProductDto> toDtoList(List<Spu> spus, boolean withSkus) {
        if (!withSkus) {
            return spus.stream().map(this::toDto).toList();
        }
        List<Long> spuIds = spus.stream().map(Spu::getId).toList();
        List<Sku> allSkus = skuRepository.findBySpuIdIn(spuIds);
        Map<Long, List<Sku>> skusBySpuId = allSkus.stream()
            .collect(Collectors.groupingBy(Sku::getSpuId));
        return spus.stream()
            .map(spu -> toDtoWithSkus(spu, skusBySpuId.getOrDefault(spu.getId(), List.of())))
            .toList();
    }

    private ProductDto toDtoWithSkus(Spu spu, List<Sku> skus) {
        List<ProductImage> defaultImages = specDimensionApplicationService.getDefaultDisplayImages(spu.getId());
        String coverImageUrl = defaultImages.isEmpty() ? null : toImageDto(defaultImages.get(0)).imageUrl();
        List<ProductImageDto> defaultDisplayImages = defaultImages.stream()
            .map(this::toImageDto).toList();
        List<SkuDto> skuDtos = skus.stream()
            .map(sku -> toSkuDto(sku, spu.getName(), spu.getProductType()))
            .toList();
        return new ProductDto(
            spu.getId(), spu.getCategoryId(), spu.getName(), spu.getDescription(),
            spu.getProductType(), spu.getServiceKind(),
            coverImageUrl, defaultDisplayImages, skuDtos
        );
    }

    private SkuDto toSkuDto(Sku sku, String spuName, String productType) {
        List<SkuSpecValueDto> specValues = skuApplicationService.resolveSpecValues(sku.getSpecOptionIds()).stream()
            .map(v -> new SkuSpecValueDto(v.dimensionName(), v.optionValue()))
            .toList();
        return new SkuDto(
            sku.getId(), sku.getSpuId(), sku.getPriceCents(),
            sku.getDisplayName(), spuName, productType, specValues
        );
    }

    private ProductDto toDto(Spu s) {
        List<ProductImage> defaultImages = specDimensionApplicationService.getDefaultDisplayImages(s.getId());
        String coverImageUrl = defaultImages.isEmpty() ? null : toImageDto(defaultImages.get(0)).imageUrl();
        List<ProductImageDto> defaultDisplayImages = defaultImages.stream()
            .map(this::toImageDto)
            .toList();
        return new ProductDto(
            s.getId(),
            s.getCategoryId(),
            s.getName(),
            s.getDescription(),
            s.getProductType(),
            s.getServiceKind(),
            coverImageUrl,
            defaultDisplayImages
        );
    }

    private ProductImageDto toImageDto(ProductImage img) {
        String imageUrl = img.getImageUrl();
        if (fileStorageService != null) {
            imageUrl = fileStorageService.toServeUrlIfMinio(imageUrl);
        }
        return new ProductImageDto(
            img.getId(),
            img.getSpuId(),
            img.getSpecOptionId(),
            imageUrl,
            img.getSortOrder()
        );
    }
}
