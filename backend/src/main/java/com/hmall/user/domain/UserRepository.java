package com.hmall.user.domain;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储：持久化与查询用户。
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAll();

    void deleteById(Long id);
}
