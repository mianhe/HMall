package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * 类别仓储：持久化与查询类别。
 * 子类别不存储于聚合内，通过 findByParentId 查询。
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    /**
     * 按父类别 ID 查询子类别列表。
     * @param parentId 为 null 表示查根目录下所有类别
     */
    List<Category> findByParentId(Long parentId);

    /** 是否存在以给定 ID 为父类别的子类别（用于判断是否叶子节点） */
    boolean existsByParentId(Long parentId);

    List<Category> findAll();

    void deleteById(Long id);
}
