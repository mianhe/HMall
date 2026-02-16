package com.hmall.user.domain;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(Long addressId);

    List<Address> findByUserId(Long userId);

    void deleteById(Long addressId);
}
