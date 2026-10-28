package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class UserPreferenceRequest {

    @JsonProperty("org_id")
    private UUID orgId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("language")
    private String language;

    public UserPreferenceRequest() {}

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
