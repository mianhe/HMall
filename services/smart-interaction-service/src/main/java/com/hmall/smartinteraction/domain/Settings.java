package com.hmall.smartinteraction.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    private Long id = 1L;

    @Column(columnDefinition = "TEXT")
    private String adminBasePrompt;

    @Column(columnDefinition = "TEXT")
    private String consumerBasePrompt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Settings() {}

    public static Settings createDefault(String defaultAdminPrompt, String defaultConsumerPrompt) {
        Settings s = new Settings();
        s.id = 1L;
        s.adminBasePrompt = defaultAdminPrompt;
        s.consumerBasePrompt = defaultConsumerPrompt;
        s.updatedAt = Instant.now();
        return s;
    }

    public void update(String adminBasePrompt, String consumerBasePrompt) {
        this.adminBasePrompt = normalizePrompt(adminBasePrompt);
        this.consumerBasePrompt = normalizePrompt(consumerBasePrompt);
        this.updatedAt = Instant.now();
    }

    public void resetToDefaults(String defaultAdminPrompt, String defaultConsumerPrompt) {
        this.adminBasePrompt = defaultAdminPrompt;
        this.consumerBasePrompt = defaultConsumerPrompt;
        this.updatedAt = Instant.now();
    }

    private String normalizePrompt(String prompt) {
        return (prompt != null && !prompt.isBlank()) ? prompt : null;
    }

    public Long getId() { return id; }
    public String getAdminBasePrompt() { return adminBasePrompt; }
    public String getConsumerBasePrompt() { return consumerBasePrompt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
