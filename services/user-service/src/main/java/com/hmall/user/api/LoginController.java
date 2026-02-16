package com.hmall.user.api;

import com.hmall.user.api.dto.LoginRequestDto;
import com.hmall.user.api.dto.LoginResponseDto;
import com.hmall.user.application.LoginApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final LoginApplicationService loginApplicationService;

    public LoginController(LoginApplicationService loginApplicationService) {
        this.loginApplicationService = loginApplicationService;
    }

    @PostMapping
    public LoginResponseDto login(@RequestBody LoginRequestDto dto) {
        String token = loginApplicationService.login(dto.username(), dto.password());
        return new LoginResponseDto(token);
    }
}
