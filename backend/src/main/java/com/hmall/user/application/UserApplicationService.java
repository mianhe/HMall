package com.hmall.user.application;

import com.hmall.user.domain.User;
import com.hmall.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理用户应用服务：创建、查询用户。
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new UserBadRequestException("用户名为空");
        }
        if (password == null || password.isBlank()) {
            throw new UserBadRequestException("密码为空");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameExistsException("用户名已存在");
        }
        String passwordHash = passwordEncoder.encode(password);
        User user = new User(username, passwordHash);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll();
    }
}
