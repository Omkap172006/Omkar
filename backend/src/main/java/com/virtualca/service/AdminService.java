package com.virtualca.service;

import com.virtualca.dto.DbStatusResponse;
import com.virtualca.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminService {

    private final DataSource dataSource;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherEntryRepository voucherEntryRepository;
    private final ComplianceEventRepository complianceEventRepository;
    private final AiFinancialInsightRepository aiFinancialInsightRepository;

    @Value("${virtualca.admin.secret-key:admin2026}")
    private String adminSecretKey;

    @Value("${virtualca.admin.dev-key:dev2026}")
    private String devSecretKey;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/virtual_ca_db}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:postgres}")
    private String datasourceUser;

    public AdminService(
            DataSource dataSource,
            OrganizationRepository organizationRepository,
            AccountRepository accountRepository,
            VoucherRepository voucherRepository,
            VoucherEntryRepository voucherEntryRepository,
            ComplianceEventRepository complianceEventRepository,
            AiFinancialInsightRepository aiFinancialInsightRepository
    ) {
        this.dataSource = dataSource;
        this.organizationRepository = organizationRepository;
        this.accountRepository = accountRepository;
        this.voucherRepository = voucherRepository;
        this.voucherEntryRepository = voucherEntryRepository;
        this.complianceEventRepository = complianceEventRepository;
        this.aiFinancialInsightRepository = aiFinancialInsightRepository;
    }

    public boolean isAdmin(String adminHeader, String authHeader, String paramKey) {
        if (adminHeader != null && (adminHeader.trim().equals(adminSecretKey) || adminHeader.trim().equals(devSecretKey))) {
            return true;
        }
        if (authHeader != null && authHeader.trim().toLowerCase().startsWith("bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token.equals(adminSecretKey) || token.equals(devSecretKey)) {
                return true;
            }
        }
        if (paramKey != null && (paramKey.trim().equals(adminSecretKey) || paramKey.trim().equals(devSecretKey))) {
            return true;
        }
        return false;
    }

    public DbStatusResponse getDbStatus() {
        DbStatusResponse resp = new DbStatusResponse();
        resp.setAdmin(true);

        // Parse host/port/database from URL
        String url = datasourceUrl;
        resp.setDatabase("virtual_ca_db");
        resp.setHost("localhost");
        resp.setPort("5432");
        resp.setUser(datasourceUser);

        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            double latency = (System.nanoTime() - start) / 1_000_000.0;
            resp.setLatencyMs(Math.round(latency * 100.0) / 100.0);
            resp.setConnected(true);

            DatabaseMetaData meta = conn.getMetaData();
            String productName = meta.getDatabaseProductName();
            String productVersion = meta.getDatabaseProductVersion();
            resp.setEngine(productName + " " + meta.getDatabaseMajorVersion());
            resp.setServerVersion(productName + " " + productVersion);

            int active = 1;
            try (Statement stmt = conn.createStatement()) {
                if (productName.toLowerCase().contains("postgres")) {
                    ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()");
                    if (rs.next()) active = rs.getInt(1);
                }
            } catch (Exception ignored) {}
            resp.setActiveConnections(active);

            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("organizations", organizationRepository.count());
            counts.put("accounts", accountRepository.count());
            counts.put("vouchers", voucherRepository.count());
            counts.put("voucher_entries", voucherEntryRepository.count());
            counts.put("compliance_events", complianceEventRepository.count());
            counts.put("ai_financial_insights", aiFinancialInsightRepository.count());
            resp.setTableCounts(counts);

        } catch (Exception e) {
            resp.setConnected(false);
            resp.setError(e.getMessage());
            resp.setTableCounts(Map.of());
        }
        return resp;
    }
}

