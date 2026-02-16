package com.hmall.user.acceptance;

import com.hmall.user.infrastructure.persistence.AddressJpaRepository;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;
import io.cucumber.java.Before;

public class DatabaseResetHook {

    private final UserJpaRepository userJpaRepository;
    private final AddressJpaRepository addressJpaRepository;

    public DatabaseResetHook(UserJpaRepository userJpaRepository, AddressJpaRepository addressJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.addressJpaRepository = addressJpaRepository;
    }

    @Before
    public void resetDb() {
        addressJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }
}
