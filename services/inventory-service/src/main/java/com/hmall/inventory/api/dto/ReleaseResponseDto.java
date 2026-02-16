package com.hmall.inventory.api.dto;

/**
 * 释放成功响应，与 api.yaml ReleaseResponse 一致。
 */
public record ReleaseResponseDto(boolean success) {
    public static ReleaseResponseDto ok() {
        return new ReleaseResponseDto(true);
    }
}
