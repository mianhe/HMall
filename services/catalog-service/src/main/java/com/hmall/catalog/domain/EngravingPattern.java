package com.hmall.catalog.domain;

import java.util.Objects;

public class EngravingPattern {
    private final Long id;
    private String name;
    private String imageUrl;
    private Integer sortOrder;
    private boolean enabled;

    /** 新建 */
    public EngravingPattern(String name, String imageUrl, Integer sortOrder, Boolean enabled) {
        this.id = null;
        this.name = Objects.requireNonNull(name, "name");
        this.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.enabled = enabled != null ? enabled : true;
    }

    /** 从持久化还原 */
    public EngravingPattern(Long id, String name, String imageUrl, Integer sortOrder, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl");
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.enabled = enabled;
    }

    public void update(String name, String imageUrl, Integer sortOrder, Boolean enabled) {
        if (name != null) this.name = name;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (enabled != null) this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public boolean isEnabled() { return enabled; }
}
