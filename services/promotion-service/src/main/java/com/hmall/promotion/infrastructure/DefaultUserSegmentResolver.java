package com.hmall.promotion.infrastructure;

import com.hmall.promotion.application.UserSegmentResolver;
import com.hmall.promotion.application.UserSegments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UserSegmentResolver.class)
public class DefaultUserSegmentResolver implements UserSegmentResolver {
    @Override
    public UserSegments resolve(Long userId) {
        return new UserSegments("L1", java.util.Set.of());
    }
}
