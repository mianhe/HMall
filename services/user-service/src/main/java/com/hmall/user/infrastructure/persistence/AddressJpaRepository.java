package com.hmall.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByUserIdOrderByIdAsc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
