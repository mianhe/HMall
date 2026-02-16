package com.hmall.user.api;

import com.hmall.user.api.dto.UserCreateDto;
import com.hmall.user.api.dto.UserDto;
import com.hmall.user.application.UserApplicationService;
import com.hmall.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService applicationService;

    public UserController(UserApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserCreateDto dto) {
        User created = applicationService.create(dto.username(), dto.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping
    public List<UserDto> list() {
        return applicationService.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        User user = applicationService.getById(id);
        return ResponseEntity.ok(toDto(user));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername());
    }
}
