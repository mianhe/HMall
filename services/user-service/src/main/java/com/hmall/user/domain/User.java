package com.hmall.user.domain;

import java.util.Objects;

public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;

    public User(String username, String passwordHash) {
        this.id = null;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public User(Long id, String username, String passwordHash) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
