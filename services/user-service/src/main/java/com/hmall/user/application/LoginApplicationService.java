package com.hmall.user.application;

import com.hmall.user.domain.User;
import com.hmall.user.domain.UserRepository;
import com.hmall.user.infrastructure.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginApplicationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public String login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new UserBadRequestException("用户名为空");
        }
        if (password == null || password.isBlank()) {
            throw new UserBadRequestException("密码为空");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new LoginFailedException("用户不存在或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new LoginFailedException("用户不存在或密码错误");
        }
        return jwtTokenService.createToken(user.getId(), user.getUsername());
    }
}
