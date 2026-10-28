package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DbStatusResponse {
    private boolean connected;
    private String database;
    private String host;
    private String port;
    private String user;
    private String engine;

    @JsonProperty("server_version")
    private String serverVersion;

    @JsonProperty("latency_ms")
    private Double latencyMs;

    @JsonProperty("active_connections")
    private Integer activeConnections;

    @JsonProperty("table_counts")
    private Map<String, Long> tableCounts;

    @JsonProperty("isAdmin")
    private boolean isAdmin = true;

    private String error;

    public DbStatusResponse() {}

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }

    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }

    public Double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Double latencyMs) { this.latencyMs = latencyMs; }

    public Integer getActiveConnections() { return activeConnections; }
    public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }

    public Map<String, Long> getTableCounts() { return tableCounts; }
    public void setTableCounts(Map<String, Long> tableCounts) { this.tableCounts = tableCounts; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

