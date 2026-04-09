package com.hmall.user.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_segment_rule")
public class SegmentRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(name = "levels_in_csv", length = 1000)
    private String levelsInCsv;

    @Column(name = "tags_any_csv", length = 1000)
    private String tagsAnyCsv;

    @Column(name = "tags_all_csv", length = 1000)
    private String tagsAllCsv;

    @Column(name = "exclude_tags_csv", length = 1000)
    private String excludeTagsCsv;

    @Column(name = "last_preview_count")
    private Long lastPreviewCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLevelsInCsv() {
        return levelsInCsv;
    }

    public void setLevelsInCsv(String levelsInCsv) {
        this.levelsInCsv = levelsInCsv;
    }

    public String getTagsAnyCsv() {
        return tagsAnyCsv;
    }

    public void setTagsAnyCsv(String tagsAnyCsv) {
        this.tagsAnyCsv = tagsAnyCsv;
    }

    public String getTagsAllCsv() {
        return tagsAllCsv;
    }

    public void setTagsAllCsv(String tagsAllCsv) {
        this.tagsAllCsv = tagsAllCsv;
    }

    public String getExcludeTagsCsv() {
        return excludeTagsCsv;
    }

    public void setExcludeTagsCsv(String excludeTagsCsv) {
        this.excludeTagsCsv = excludeTagsCsv;
    }

    public Long getLastPreviewCount() {
        return lastPreviewCount;
    }

    public void setLastPreviewCount(Long lastPreviewCount) {
        this.lastPreviewCount = lastPreviewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
