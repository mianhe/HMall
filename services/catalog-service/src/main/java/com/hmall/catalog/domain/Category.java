package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * 类别聚合根。
 * 父类别 ID 为 null 表示根类别；子类别通过「父类别 ID = 本类别 ID」查询得出，不在此存储。
 */
public class Category {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final String description;

    /** 新建（尚未持久化，id 为 null） */
    public Category(Long parentId, String name, String description) {
        this.id = null;
        this.parentId = parentId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
    }

    /** 从持久化还原 */
    public Category(Long id, Long parentId, String name, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.parentId = parentId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRoot() {
        return parentId == null;
    }
}
