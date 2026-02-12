package com.hmall.user.domain;

import java.util.Objects;

/**
 * 用户聚合根。
 * 不变式：username 必填、全局唯一；passwordHash 必填。
 */
public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;

    /** 新建（尚未持久化，id 为 null） */
    public User(String username, String passwordHash) {
        this.id = null;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    /** 从持久化还原 */
    public User(Long id, String username, String passwordHash) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
