package com.hmall.catalog.api;

import com.hmall.catalog.api.dto.CategoryCreateDto;
import com.hmall.catalog.api.dto.CategoryDto;
import com.hmall.catalog.api.dto.CategoryUpdateDto;
import com.hmall.catalog.application.CategoryApplicationService;
import com.hmall.catalog.domain.Category;
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

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
        Category category = applicationService.getById(id);
        return ResponseEntity.ok(toDto(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @RequestBody CategoryUpdateDto dto) {
        Category updated = applicationService.update(id, dto.name(), dto.description());
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getParentId(), c.getName(), c.getDescription());
    }
}
