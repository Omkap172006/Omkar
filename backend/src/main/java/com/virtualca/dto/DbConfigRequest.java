package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DbConfigRequest {
    private String host;
    private Object port;
    private String user;
    private String password;
    private String database;

    @JsonProperty("admin_key")
    private String adminKey;

    public DbConfigRequest() {}

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Object getPort() { return port; }
    public void setPort(Object port) { this.port = port; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getAdminKey() { return adminKey; }
    public void setAdminKey(String adminKey) { this.adminKey = adminKey; }
}

