package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequest {
    private String email;

    @JsonProperty("admin_key")
    private String adminKey;

    @JsonProperty("dev_key")
    private String devKey;

    public LoginRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdminKey() { return adminKey; }
    public void setAdminKey(String adminKey) { this.adminKey = adminKey; }

    public String getDevKey() { return devKey; }
    public void setDevKey(String devKey) { this.devKey = devKey; }

    public String getEffectiveKey() {
        if (adminKey != null && !adminKey.isBlank()) return adminKey.trim();
        if (devKey != null && !devKey.isBlank()) return devKey.trim();
        return null;
    }
}

