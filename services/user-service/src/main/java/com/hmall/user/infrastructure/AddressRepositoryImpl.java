package com.hmall.user.infrastructure;

import com.hmall.user.domain.Address;
import com.hmall.user.domain.AddressRepository;
import com.hmall.user.infrastructure.persistence.AddressEntity;
import com.hmall.user.infrastructure.persistence.AddressJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository jpaRepository;

    public AddressRepositoryImpl(AddressJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = toEntity(address);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Address> findById(Long addressId) {
        return jpaRepository.findById(addressId)
            .map(this::toDomain);
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByIdAsc(userId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long addressId) {
        jpaRepository.deleteById(addressId);
    }

    private AddressEntity toEntity(Address a) {
        AddressEntity e = new AddressEntity();
        if (a.getAddressId() != null) {
            e.setId(a.getAddressId());
        }
        e.setUserId(a.getUserId());
        e.setRecipientName(a.getRecipientName());
        e.setPhone(a.getPhone());
        e.setProvince(a.getProvince());
        e.setCity(a.getCity());
        e.setDistrict(a.getDistrict());
        e.setDetail(a.getDetail());
        return e;
    }

    private Address toDomain(AddressEntity e) {
        return new Address(
            e.getId(),
            e.getUserId(),
            e.getRecipientName(),
            e.getPhone(),
            e.getProvince(),
            e.getCity(),
            e.getDistrict(),
            e.getDetail()
        );
    }
}
