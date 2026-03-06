package com.hmall.fulfillment.domain;

import java.util.Objects;

/**
 * 镭雕内容，附属在实体履约单上。
 * 不变式：至少 patternId 或 text 其一非空。
 */
public class EngravingInfo {

    private final Long patternId;
    private final String patternName;
    private final String text;

    public EngravingInfo(Long patternId, String patternName, String text) {
        if (patternId == null && (text == null || text.isBlank())) {
            throw new IllegalArgumentException("至少 patternId 或 text 其一非空");
        }
        this.patternId = patternId;
        this.patternName = patternName;
        this.text = text;
    }

    public Long getPatternId() { return patternId; }
    public String getPatternName() { return patternName; }
    public String getText() { return text; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngravingInfo that = (EngravingInfo) o;
        return Objects.equals(patternId, that.patternId)
            && Objects.equals(patternName, that.patternName)
            && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() { return Objects.hash(patternId, patternName, text); }
}
