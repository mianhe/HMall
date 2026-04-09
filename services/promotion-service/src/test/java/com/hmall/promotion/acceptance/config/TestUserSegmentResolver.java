package com.hmall.promotion.acceptance.config;

import com.hmall.promotion.application.UserSegmentResolver;
import com.hmall.promotion.application.UserSegments;

import java.util.Map;
import java.util.Set;

public class TestUserSegmentResolver implements UserSegmentResolver {
    private final Map<Long, UserSegments> segmentsByUserId = new java.util.concurrent.ConcurrentHashMap<>();

    public TestUserSegmentResolver() {
        segmentsByUserId.put(1001L, new UserSegments("L1", Set.of("NEW_USER")));
        segmentsByUserId.put(2002L, new UserSegments("L2", Set.of("VIP")));
    }

    @Override
    public UserSegments resolve(Long userId) {
        return segmentsByUserId.getOrDefault(userId, new UserSegments("L1", Set.of()));
    }
}
