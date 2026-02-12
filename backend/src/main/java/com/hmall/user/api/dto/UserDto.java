package com.hmall.user.api.dto;

/**
 * 用户响应，与 user api.yaml User 一致。不含 passwordHash。
 */
public record UserDto(Long id, String username) {}
