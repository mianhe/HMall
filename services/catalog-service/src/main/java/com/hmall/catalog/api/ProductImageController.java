package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.ProductImageCreateDto;
import com.hmall.catalog.api.dto.ProductImageDto;
import com.hmall.catalog.application.SpecDimensionApplicationService;
import com.hmall.catalog.domain.ProductImage;
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
@RequestMapping("/api/products/{spuId}/images")
public class ProductImageController {

    private final SpecDimensionApplicationService applicationService;
    private final FileStorageService fileStorageService;

    public ProductImageController(
            SpecDimensionApplicationService applicationService,
            @Autowired(required = false) FileStorageService fileStorageService) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ProductImageDto> addProductImage(
            @PathVariable Long spuId,
            @RequestBody ProductImageCreateDto dto) {
        ProductImage created = applicationService.addProductImage(
            spuId,
            dto.imageUrl(),
            dto.sortOrder()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductImageDto>> listProductImages(@PathVariable Long spuId) {
        List<ProductImage> images = applicationService.listProductImages(spuId);
        return ResponseEntity.ok(images.stream().map(this::toDto).toList());
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long spuId,
            @PathVariable Long imageId) {
        applicationService.deleteProductImage(imageId);
        return ResponseEntity.noContent().build();
    }

    private ProductImageDto toDto(ProductImage img) {
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
