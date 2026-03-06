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
    private final String productType;
    /** 服务分类：ENGRAVING=镭雕，WARRANTY=延保，INSURANCE=碎屏险等，OTHER=其他；仅 SERVICE 类型有效，null 视为 OTHER */
    private final String serviceKind;

    /** 新建（尚未持久化，id 为 null） */
    public Spu(Long categoryId, String name, String description, String productType, String serviceKind) {
        this.id = null;
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.productType = productType != null ? productType : "PHYSICAL";
        this.serviceKind = (serviceKind != null && !serviceKind.isBlank()) ? serviceKind : "OTHER";
    }

    /** 从持久化还原 */
    public Spu(Long id, Long categoryId, String name, String description, String productType, String serviceKind) {
        this.id = Objects.requireNonNull(id, "id");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.productType = productType != null ? productType : "PHYSICAL";
        this.serviceKind = (serviceKind != null && !serviceKind.isBlank()) ? serviceKind : "OTHER";
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getProductType() { return productType; }
    public String getServiceKind() { return serviceKind; }
}
