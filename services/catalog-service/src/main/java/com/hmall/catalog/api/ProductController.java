package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ProductCreateDto;
import com.hmall.catalog.api.dto.ProductDto;
import com.hmall.catalog.api.dto.ProductImageDto;
import com.hmall.catalog.api.dto.ProductUpdateDto;
import com.hmall.catalog.application.SpecDimensionApplicationService;
import com.hmall.catalog.application.SpuApplicationService;
import com.hmall.catalog.domain.ProductImage;
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

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final SpuApplicationService applicationService;
    private final SpecDimensionApplicationService specDimensionApplicationService;
    private final FileStorageService fileStorageService;

    public ProductController(
            SpuApplicationService applicationService,
            SpecDimensionApplicationService specDimensionApplicationService,
            @Autowired(required = false) FileStorageService fileStorageService) {
        this.applicationService = applicationService;
        this.specDimensionApplicationService = specDimensionApplicationService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@RequestBody ProductCreateDto dto) {
        Spu created = applicationService.create(
            dto.categoryId(),
            dto.name(),
            dto.description()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDto(created));
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam Long categoryId) {
        return applicationService.listByCategoryId(categoryId).stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/search")
    public List<ProductDto> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        return applicationService.searchByName(keyword).stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        Spu spu = applicationService.getById(id);
        return ResponseEntity.ok(toDto(spu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody ProductUpdateDto dto) {
        Spu updated = applicationService.update(id, dto.name(), dto.description());
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
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
