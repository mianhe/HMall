package com.hmall.user.api.dto;

/**
 * 创建用户请求，与 user api.yaml UserCreate 一致。
 */
public record UserCreateDto(String username, String password) {}
