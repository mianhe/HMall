package com.hmall.user.domain;

import java.util.Objects;
import java.util.Set;

public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String level;
    private final Set<String> tags;

    public User(String username, String passwordHash) {
        this(null, username, passwordHash, "L1", Set.of());
    }

    public User(Long id, String username, String passwordHash) {
        this(id, username, passwordHash, "L1", Set.of());
    }

    public User(String username, String passwordHash, String level, Set<String> tags) {
        this(null, username, passwordHash, level, tags);
    }

    public User(Long id, String username, String passwordHash, String level, Set<String> tags) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.level = level == null || level.isBlank() ? "L1" : level;
        this.tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getLevel() { return level; }
    public Set<String> getTags() { return tags; }

    public User updateLevel(String newLevel) {
        String normalized = newLevel == null || newLevel.isBlank() ? "L1" : newLevel.trim();
        return new User(id, username, passwordHash, normalized, tags);
    }

    public User replaceTags(Set<String> newTags) {
        return new User(id, username, passwordHash, level, newTags);
    }
}
