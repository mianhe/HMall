package com.hmall.catalog.domain;

import java.util.Objects;

/**
 * 产品展示图值对象。可挂在产品本身（specOptionId 为空）或某规格选项下。
 */
public class ProductImage {

    private final Long id;
    private final Long spuId;
    private final Long specOptionId;
    private final String imageUrl;
    private final Integer sortOrder;

    /** 新建产品级展示图（尚未持久化，id 为 null） */
    public static ProductImage productLevel(Long spuId, String imageUrl, Integer sortOrder) {
        return new ProductImage(null, spuId, null, imageUrl, sortOrder);
    }

    /** 新建选项级展示图（尚未持久化，id 为 null） */
    public static ProductImage optionLevel(Long spuId, Long specOptionId, String imageUrl, Integer sortOrder) {
        return new ProductImage(null, spuId, Objects.requireNonNull(specOptionId, "specOptionId"), imageUrl, sortOrder);
    }

    /** 从持久化还原 */
    public ProductImage(Long id, Long spuId, Long specOptionId, String imageUrl, Integer sortOrder) {
        this.id = id;
        this.spuId = Objects.requireNonNull(spuId, "spuId");
        this.specOptionId = specOptionId;
        this.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl");
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getSpuId() {
        return spuId;
    }

    public Long getSpecOptionId() {
        return specOptionId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
