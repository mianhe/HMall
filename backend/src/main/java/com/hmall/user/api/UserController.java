package com.hmall.user.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口。占位 Controller，返回空列表，保证 BC 骨架可启动。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public ResponseEntity<List<?>> list() {
        return ResponseEntity.ok(List.of());
    }
}
