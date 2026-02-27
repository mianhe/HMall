package com.hmall.catalog.infrastructure;

import com.hmall.catalog.domain.ServiceBinding;
import com.hmall.catalog.domain.ServiceBindingRepository;
import com.hmall.catalog.infrastructure.persistence.ServiceBindingEntity;
import com.hmall.catalog.infrastructure.persistence.ServiceBindingJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ServiceBindingRepositoryImpl implements ServiceBindingRepository {

    private final ServiceBindingJpaRepository jpaRepository;

    public ServiceBindingRepositoryImpl(ServiceBindingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ServiceBinding save(ServiceBinding binding) {
        ServiceBindingEntity entity = toEntity(binding);
        ServiceBindingEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ServiceBinding> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ServiceBinding> findByTargetSpuId(Long targetSpuId) {
        return jpaRepository.findByTargetSpuId(targetSpuId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<ServiceBinding> findByServiceSkuId(Long serviceSkuId) {
        return jpaRepository.findByServiceSkuId(serviceSkuId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private ServiceBindingEntity toEntity(ServiceBinding domain) {
        ServiceBindingEntity e = new ServiceBindingEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setServiceSkuId(domain.getServiceSkuId());
        e.setTargetSpuId(domain.getTargetSpuId());
        e.setPriceCents(domain.getPriceCents());
        return e;
    }

    private ServiceBinding toDomain(ServiceBindingEntity entity) {
        return new ServiceBinding(
            entity.getId(),
            entity.getServiceSkuId(),
            entity.getTargetSpuId(),
            entity.getPriceCents()
        );
    }
}
