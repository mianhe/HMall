package com.hmall.user.application;

import com.hmall.user.domain.SegmentCondition;
import com.hmall.user.domain.SegmentRule;
import com.hmall.user.domain.SegmentRuleRepository;
import com.hmall.user.domain.User;
import com.hmall.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final SegmentRuleRepository segmentRuleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ALLOWED_LEVELS = Set.of("L1", "L2", "L3");

    public UserApplicationService(
        UserRepository userRepository,
        SegmentRuleRepository segmentRuleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.segmentRuleRepository = segmentRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new UserBadRequestException("用户名为空");
        }
        if (password == null || password.isBlank()) {
            throw new UserBadRequestException("密码为空");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameExistsException("用户名已存在");
        }
        String passwordHash = passwordEncoder.encode(password);
        User user = new User(username, passwordHash);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateLevel(Long userId, String level) {
        String normalizedLevel = normalizeLevel(level);
        User user = getById(userId);
        User updated = user.updateLevel(normalizedLevel);
        return userRepository.save(updated);
    }

    @Transactional
    public User replaceTags(Long userId, Set<String> tags) {
        User user = getById(userId);
        User updated = user.replaceTags(normalizeTags(tags));
        return userRepository.save(updated);
    }

    @Transactional
    public SegmentRule createSegmentRule(String name, SegmentCondition condition) {
        SegmentRule rule = new SegmentRule(name, condition);
        return segmentRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<SegmentRule> listSegmentRules() {
        return segmentRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SegmentRule getSegmentRule(Long ruleId) {
        return segmentRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("圈选规则不存在"));
    }

    @Transactional
    public SegmentRulePreviewResult previewSegmentRule(Long ruleId, Integer sampleSize) {
        SegmentRule rule = getSegmentRule(ruleId);
        int size = normalizeSampleSize(sampleSize);
        List<User> users = userRepository.findAll();
        List<Long> sampleUserIds = new ArrayList<>();
        Map<String, Long> reasonCounter = new LinkedHashMap<>();
        long hitCount = 0L;
        for (User user : users) {
            List<String> reasons = rule.getConditions().mismatchReasons(user.getLevel(), user.getTags());
            if (reasons.isEmpty()) {
                hitCount++;
                if (sampleUserIds.size() < size) {
                    sampleUserIds.add(user.getId());
                }
                continue;
            }
            for (String reason : reasons) {
                reasonCounter.merge(reason, 1L, Long::sum);
            }
        }
        segmentRuleRepository.save(rule.withPreviewCount(hitCount));
        List<ReasonStat> stats = reasonCounter.entrySet().stream()
            .map(entry -> new ReasonStat(entry.getKey(), entry.getValue()))
            .toList();
        return new SegmentRulePreviewResult(ruleId, hitCount, List.copyOf(sampleUserIds), stats);
    }

    @Transactional
    public SegmentRule activateSegmentRule(Long ruleId) {
        SegmentRule rule = getSegmentRule(ruleId);
        return segmentRuleRepository.save(rule.activate());
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            throw new UserBadRequestException("用户等级不能为空");
        }
        String normalized = level.trim();
        if (!ALLOWED_LEVELS.contains(normalized)) {
            throw new UserBadRequestException("用户等级不合法，仅支持 L1/L2/L3");
        }
        return normalized;
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
            .filter(java.util.Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static int normalizeSampleSize(Integer sampleSize) {
        if (sampleSize == null) {
            return 20;
        }
        if (sampleSize < 1 || sampleSize > 200) {
            throw new UserBadRequestException("sampleSize 必须在 1 到 200 之间");
        }
        return sampleSize;
    }
}
