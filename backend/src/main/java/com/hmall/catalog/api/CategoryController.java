package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.CategoryCreateDto;
import com.hmall.catalog.api.dto.CategoryDto;
import com.hmall.catalog.application.CategoryApplicationService;
import com.hmall.catalog.domain.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryApplicationService applicationService;

    public CategoryController(CategoryApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@RequestBody CategoryCreateDto dto) {
        Category created = applicationService.create(
            dto.parentId(),
            dto.name(),
            dto.description()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDto(created));
    }

    @GetMapping
    public List<CategoryDto> list(@RequestParam(required = false) Long parentId) {
        return applicationService.listByParentId(parentId).stream()
            .map(this::toDto)
            .toList();
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getParentId(), c.getName(), c.getDescription());
    }
}
