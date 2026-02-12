package com.hmall.user.api.dto;

/**
 * 登录请求，与 user api.yaml LoginRequest 一致。
 */
public record LoginRequestDto(String username, String password) {}
