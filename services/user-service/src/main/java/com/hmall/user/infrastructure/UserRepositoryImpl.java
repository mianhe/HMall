package com.hmall.user.infrastructure;

import com.hmall.user.domain.User;
import com.hmall.user.domain.UserRepository;
import com.hmall.user.infrastructure.persistence.UserEntity;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private UserEntity toEntity(User domain) {
        UserEntity e = new UserEntity();
        if (domain.getId() != null) e.setId(domain.getId());
        e.setUsername(domain.getUsername());
        e.setPasswordHash(domain.getPasswordHash());
        e.setLevel(domain.getLevel());
        e.setTagsCsv(domain.getTags().isEmpty() ? null : String.join(",", domain.getTags()));
        return e;
    }

    private User toDomain(UserEntity entity) {
        Set<String> tags = entity.getTagsCsv() == null || entity.getTagsCsv().isBlank()
            ? Set.of()
            : java.util.Arrays.stream(entity.getTagsCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getLevel(), tags);
    }
}
