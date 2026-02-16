package com.hmall.inventory.api.dto;

/**
 * 占用成功响应，与 api.yaml OccupyResponse 一致。
 */
public record OccupyResponseDto(boolean success) {
    public static OccupyResponseDto ok() {
        return new OccupyResponseDto(true);
    }
}
