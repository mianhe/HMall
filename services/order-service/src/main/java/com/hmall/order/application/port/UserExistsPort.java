package com.hmall.order.application.port;

/**
 * 用户存在性端口，供 Order 校验 userId 是否存在于 User BC。
 */
public interface UserExistsPort {

    /**
     * 校验用户是否存在。
     * @throws IllegalArgumentException 当用户不存在时（对应 404）
     */
    void requireExists(Long userId);
}
