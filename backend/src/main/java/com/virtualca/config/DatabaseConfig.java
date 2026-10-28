package com.virtualca.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_NAME:virtual_ca_db}")
    private String dbName;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:Atharva06}")
    private String dbPassword;

    @Value("${spring.datasource.url:}")
    private String explicitUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (explicitUrl != null && !explicitUrl.isBlank() && explicitUrl.contains("h2")) {
            log.info("Detected explicit H2 test datasource ({}). Using in-memory database...", explicitUrl);
            HikariConfig testConfig = new HikariConfig();
            testConfig.setJdbcUrl(explicitUrl);
            testConfig.setDriverClassName("org.h2.Driver");
            testConfig.setUsername("sa");
            testConfig.setPassword("");
            testConfig.setMaximumPoolSize(5);
            return new HikariDataSource(testConfig);
        }

        boolean pgAvailable = isPortOpen(dbHost, dbPort, 1500);

        if (pgAvailable) {
            try {
                log.info("Detected PostgreSQL running on {}:{}. Initializing PostgreSQL DataSource...", dbHost, dbPort);
                HikariConfig pgConfig = new HikariConfig();
                pgConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName));
                pgConfig.setUsername(dbUser);
                pgConfig.setPassword(dbPassword);
                pgConfig.setDriverClassName("org.postgresql.Driver");
                pgConfig.setMaximumPoolSize(10);
                pgConfig.setMinimumIdle(2);
                pgConfig.setConnectionTimeout(3000);
                return new HikariDataSource(pgConfig);
            } catch (Exception e) {
                log.warn("Failed to connect to PostgreSQL ({}), activating file-persisted resilient storage...", e.getMessage());
            }
        } else {
            log.warn("PostgreSQL server at {}:{} is not reachable. Activating file-persisted resilient database fallback...", dbHost, dbPort);
        }

        // Persistent resilient database so records are never lost even if PostgreSQL is offline
        File dbDir = new File("database");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        File dbFile = new File(dbDir, "virtual_ca_resilient");

        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl(String.format("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH", dbFile.getAbsolutePath()));
        h2Config.setDriverClassName("org.h2.Driver");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        h2Config.setMaximumPoolSize(10);
        return new HikariDataSource(h2Config);
    }

    private boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

