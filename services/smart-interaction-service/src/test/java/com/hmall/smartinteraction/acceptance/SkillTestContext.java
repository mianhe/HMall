package com.hmall.smartinteraction.acceptance;

import java.util.HashMap;
import java.util.Map;

public class SkillTestContext {

    private int lastStatusCode;
    private Map<String, Object> lastResponseBody;
    private Long lastSkillId;
    private final Map<String, Long> skillIdsByName = new HashMap<>();

    public int getLastStatusCode() { return lastStatusCode; }
    public void setLastStatusCode(int statusCode) { this.lastStatusCode = statusCode; }

    public Map<String, Object> getLastResponseBody() { return lastResponseBody; }
    public void setLastResponseBody(Map<String, Object> body) { this.lastResponseBody = body; }

    public Long getLastSkillId() { return lastSkillId; }
    public void setLastSkillId(Long id) { this.lastSkillId = id; }

    public void putSkillId(String name, Long id) { skillIdsByName.put(name, id); }
    public Long getSkillIdByName(String name) { return skillIdsByName.get(name); }
}
