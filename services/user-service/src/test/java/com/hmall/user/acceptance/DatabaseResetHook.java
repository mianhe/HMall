package com.hmall.user.acceptance;

import com.hmall.user.infrastructure.persistence.AddressJpaRepository;
import com.hmall.user.infrastructure.persistence.SegmentRuleJpaRepository;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;
import io.cucumber.java.Before;

public class DatabaseResetHook {

    private final UserJpaRepository userJpaRepository;
    private final AddressJpaRepository addressJpaRepository;
    private final SegmentRuleJpaRepository segmentRuleJpaRepository;

    public DatabaseResetHook(
        UserJpaRepository userJpaRepository,
        AddressJpaRepository addressJpaRepository,
        SegmentRuleJpaRepository segmentRuleJpaRepository
    ) {
        this.userJpaRepository = userJpaRepository;
        this.addressJpaRepository = addressJpaRepository;
        this.segmentRuleJpaRepository = segmentRuleJpaRepository;
    }

    @Before
    public void resetDb() {
        segmentRuleJpaRepository.deleteAll();
        addressJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }
}
