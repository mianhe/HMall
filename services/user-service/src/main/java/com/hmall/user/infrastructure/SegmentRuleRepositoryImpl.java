package com.hmall.user.infrastructure;

import com.hmall.user.domain.SegmentCondition;
import com.hmall.user.domain.SegmentRule;
import com.hmall.user.domain.SegmentRuleRepository;
import com.hmall.user.domain.SegmentRuleStatus;
import com.hmall.user.infrastructure.persistence.SegmentRuleEntity;
import com.hmall.user.infrastructure.persistence.SegmentRuleJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class SegmentRuleRepositoryImpl implements SegmentRuleRepository {
    private final SegmentRuleJpaRepository jpaRepository;

    public SegmentRuleRepositoryImpl(SegmentRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SegmentRule save(SegmentRule rule) {
        SegmentRuleEntity entity = toEntity(rule);
        SegmentRuleEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public java.util.Optional<SegmentRule> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SegmentRule> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private SegmentRuleEntity toEntity(SegmentRule rule) {
        SegmentRuleEntity entity = new SegmentRuleEntity();
        if (rule.getId() != null) {
            entity.setId(rule.getId());
        }
        entity.setName(rule.getName());
        entity.setStatus(rule.getStatus().name());
        entity.setLevelsInCsv(toCsv(rule.getConditions().levelsIn()));
        entity.setTagsAnyCsv(toCsv(rule.getConditions().tagsAny()));
        entity.setTagsAllCsv(toCsv(rule.getConditions().tagsAll()));
        entity.setExcludeTagsCsv(toCsv(rule.getConditions().excludeTags()));
        entity.setLastPreviewCount(rule.getLastPreviewCount());
        entity.setCreatedAt(rule.getCreatedAt());
        entity.setUpdatedAt(rule.getUpdatedAt());
        return entity;
    }

    private SegmentRule toDomain(SegmentRuleEntity entity) {
        SegmentCondition condition = new SegmentCondition(
            parseCsv(entity.getLevelsInCsv()),
            parseCsv(entity.getTagsAnyCsv()),
            parseCsv(entity.getTagsAllCsv()),
            parseCsv(entity.getExcludeTagsCsv())
        );
        return new SegmentRule(
            entity.getId(),
            entity.getName(),
            condition,
            SegmentRuleStatus.valueOf(entity.getStatus()),
            entity.getLastPreviewCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private static String toCsv(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .forEach(values::add);
        return Set.copyOf(values);
    }
}
