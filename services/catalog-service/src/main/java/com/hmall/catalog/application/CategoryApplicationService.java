package com.hmall.catalog.application;

import com.hmall.catalog.domain.Category;
import com.hmall.catalog.domain.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理类别应用服务：创建、查询、修改、删除类别；按父类别查询列表。
 */
@Service
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(Long parentId, String name, String description) {
        if (parentId != null) {
            categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("父类别不存在"));
        }
        Category category = new Category(parentId, name, description);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> listByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return findOrThrow(id);
    }

    @Transactional
    public Category update(Long id, String name, String description) {
        Category existing = findOrThrow(id);
        Category updated = new Category(existing.getId(), existing.getParentId(), name, description);
        return categoryRepository.save(updated);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> getCategoryTree() {
        List<Category> all = categoryRepository.findAll();
        Map<Long, CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        for (Category c : all) {
            nodeMap.put(c.getId(), new CategoryTreeNode(c.getId(), c.getName(), c.getDescription(), new ArrayList<>()));
        }
        List<CategoryTreeNode> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryTreeNode node = nodeMap.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryTreeNode parent = nodeMap.get(c.getParentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }

    public record CategoryTreeNode(Long id, String name, String description, List<CategoryTreeNode> children) {}

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("类别不存在"));
    }
}
