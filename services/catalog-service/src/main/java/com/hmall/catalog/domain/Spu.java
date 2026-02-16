package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * SPU（商品）聚合根。
 * 仅叶子类别可挂 SPU；与类别为多对一，仅存类别 ID。
 */
public class Spu {

    private final Long id;
    private final Long categoryId;
    private final String name;
    private final String description;

    /** 新建（尚未持久化，id 为 null） */
    public Spu(Long categoryId, String name, String description) {
        this.id = null;
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
    }

    /** 从持久化还原 */
    public Spu(Long id, Long categoryId, String name, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
