package com.hmall.smartinteraction.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(columnDefinition = "TEXT")
    private String allowedToolsJson;

    @Column(nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Skill() {}

    public Skill(String name, String description, String systemPrompt, List<String> allowedTools) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        setAllowedTools(allowedTools);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, String systemPrompt, List<String> allowedTools) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        setAllowedTools(allowedTools);
        this.updatedAt = Instant.now();
    }

    public void setAsDefault() {
        this.isDefault = true;
        this.updatedAt = Instant.now();
    }

    public void clearDefault() {
        this.isDefault = false;
        this.updatedAt = Instant.now();
    }

    public List<String> matchTools(List<String> allToolNames) {
        List<String> allowed = getAllowedTools();
        if (allowed.isEmpty() || allowed.contains("*")) {
            return allToolNames;
        }
        List<String> result = new ArrayList<>();
        for (String toolName : allToolNames) {
            for (String pattern : allowed) {
                if (matches(pattern, toolName)) {
                    result.add(toolName);
                    break;
                }
            }
        }
        return result;
    }

    private boolean matches(String pattern, String toolName) {
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return toolName.startsWith(prefix);
        }
        return pattern.equals(toolName);
    }

    public List<String> getAllowedTools() {
        if (allowedToolsJson == null || allowedToolsJson.isBlank() || allowedToolsJson.equals("[]")) {
            return List.of();
        }
        String content = allowedToolsJson.substring(1, allowedToolsJson.length() - 1);
        List<String> result = new ArrayList<>();
        for (String item : content.split(",")) {
            String trimmed = item.trim().replaceAll("^\"|\"$", "");
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void setAllowedTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            this.allowedToolsJson = "[]";
        } else {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < tools.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(tools.get(i)).append("\"");
            }
            sb.append("]");
            this.allowedToolsJson = sb.toString();
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSystemPrompt() { return systemPrompt; }
    public boolean isDefault() { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
