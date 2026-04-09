package com.hmall.promotion.application;

public interface UserSegmentResolver {
    UserSegments resolve(Long userId);
}
