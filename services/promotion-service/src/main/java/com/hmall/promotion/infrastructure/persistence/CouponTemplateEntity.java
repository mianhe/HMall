package com.hmall.promotion.infrastructure.persistence;

import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.CouponType;
import com.hmall.promotion.domain.TemplateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "coupon_templates")
public class CouponTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(nullable = false)
    private long thresholdCents;

    private Long discountCents;

    private BigDecimal discountRate;

    private Long maxDiscountCents;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int perUserLimit;

    @Column(nullable = false)
    private int validDays;

    @Column(length = 255)
    private String targetingLevelsCsv;

    @Column(length = 1000)
    private String targetingTagsAnyCsv;

    @Column(length = 1000)
    private String targetingTagsAllCsv;

    @Column(length = 1000)
    private String targetingExcludeTagsCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected CouponTemplateEntity() {}

    public static CouponTemplateEntity fromDomain(CouponTemplate t) {
        CouponTemplateEntity e = new CouponTemplateEntity();
        e.id = t.getId();
        e.name = t.getName();
        e.type = t.getType();
        e.thresholdCents = t.getThresholdCents();
        e.discountCents = t.getDiscountCents();
        e.discountRate = t.getDiscountRate();
        e.maxDiscountCents = t.getMaxDiscountCents();
        e.totalQuantity = t.getTotalQuantity();
        e.issuedQuantity = t.getIssuedQuantity();
        e.perUserLimit = t.getPerUserLimit();
        e.validDays = t.getValidDays();
        e.targetingLevelsCsv = toCsv(t.getTargetingRule() == null ? Set.of() : t.getTargetingRule().levelsIn());
        e.targetingTagsAnyCsv = toCsv(t.getTargetingRule() == null ? Set.of() : t.getTargetingRule().tagsAny());
        e.targetingTagsAllCsv = toCsv(t.getTargetingRule() == null ? Set.of() : t.getTargetingRule().tagsAll());
        e.targetingExcludeTagsCsv = toCsv(t.getTargetingRule() == null ? Set.of() : t.getTargetingRule().excludeTags());
        e.status = t.getStatus();
        e.createdAt = t.getCreatedAt();
        return e;
    }

    public CouponTemplate toDomain() {
        CouponTemplate.TargetingRule targetingRule = null;
        Set<String> levels = toStringSet(targetingLevelsCsv);
        Set<String> tagsAny = toStringSet(targetingTagsAnyCsv);
        Set<String> tagsAll = toStringSet(targetingTagsAllCsv);
        Set<String> excludeTags = toStringSet(targetingExcludeTagsCsv);
        if (!levels.isEmpty() || !tagsAny.isEmpty() || !tagsAll.isEmpty() || !excludeTags.isEmpty()) {
            targetingRule = new CouponTemplate.TargetingRule(levels, tagsAny, tagsAll, excludeTags);
        }
        return new CouponTemplate(
                id, name, type, thresholdCents,
                discountCents, discountRate, maxDiscountCents,
                totalQuantity, issuedQuantity, perUserLimit, validDays,
                targetingRule,
                status, createdAt
        );
    }

    public Long getId() { return id; }

    private static String toCsv(Set<String> values) {
        return values == null || values.isEmpty()
                ? null
                : values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Set<String> toStringSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}
