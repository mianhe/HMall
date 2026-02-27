package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

public interface ServiceBindingRepository {
    ServiceBinding save(ServiceBinding binding);
    Optional<ServiceBinding> findById(Long id);
    List<ServiceBinding> findByTargetSpuId(Long targetSpuId);
    List<ServiceBinding> findByServiceSkuId(Long serviceSkuId);
    void deleteById(Long id);
}
