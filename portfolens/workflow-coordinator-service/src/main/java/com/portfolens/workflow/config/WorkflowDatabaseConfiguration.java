package com.portfolens.workflow.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowDatabaseConfiguration {
    @Bean DataSource workflowDataSource(
            @Value("${WORKFLOW_DATABASE_URL:${DATABASE_URL:}}") String url,
            @Value("${DB_USER:${user:${DB_USERNAME:${DATABASE_USER:}}}}") String username,
            @Value("${DATABASE_PASSWORD:${DB_PASSWORD:}}") String password,
            @Value("${host:}") String host, @Value("${port:5432}") String port,
            @Value("${database:postgres}") String database) {
        HikariDataSource source = new HikariDataSource();
        if (url == null || url.isBlank()) {
            if (host == null || host.isBlank()) throw new IllegalStateException("Configure DATABASE_URL or PostgreSQL host/port/database variables");
            source.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require");
        } else if (url.startsWith("jdbc:postgresql://")) {
            Credentials credentials = normalize(URI.create(url.substring("jdbc:".length())), source);
            if (username.isBlank()) username = credentials.username();
            if (password.isBlank()) password = credentials.password();
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            Credentials credentials = normalize(URI.create(url), source);
            if (username.isBlank()) username = credentials.username();
            if (password.isBlank()) password = credentials.password();
        } else throw new IllegalStateException("Database URL must use jdbc:postgresql://, postgresql://, or postgres://");
        source.setUsername(username); source.setPassword(password); source.setDriverClassName("org.postgresql.Driver");
        source.setMaximumPoolSize(poolInt("WORKFLOW_DB_POOL_MAX_SIZE", 2));
        source.setMinimumIdle(poolInt("WORKFLOW_DB_POOL_MIN_IDLE", 1));
        source.setConnectionTimeout(poolLong("DB_POOL_CONNECTION_TIMEOUT_MS", 30_000));
        source.setIdleTimeout(poolLong("DB_POOL_IDLE_TIMEOUT_MS", 300_000));
        source.setMaxLifetime(poolLong("DB_POOL_MAX_LIFETIME_MS", 1_200_000));
        return source;
    }

    private int poolInt(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        int parsed = Integer.parseInt(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be at least 1");
        return parsed;
    }

    private long poolLong(String name, long fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        long parsed = Long.parseLong(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be at least 1");
        return parsed;
    }

    private Credentials normalize(URI uri, HikariDataSource source) {
        String query = uri.getRawQuery();
        Map<String, String> params = new LinkedHashMap<>();
        if (query != null) for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2); params.put(decode(parts[0]), parts.length == 2 ? parts[1] : "");
        }
        params.putIfAbsent("sslmode", "require");
        String normalizedQuery = params.entrySet().stream().map(e -> e.getKey() + (e.getValue().isEmpty() ? "" : "=" + e.getValue())).reduce((a, b) -> a + "&" + b).orElse("sslmode=require");
        String authority = uri.getRawAuthority(); String userInfo = uri.getRawUserInfo();
        source.setJdbcUrl("jdbc:postgresql://" + authority.substring(authority.lastIndexOf('@') + 1) + uri.getRawPath() + "?" + normalizedQuery);
        if (userInfo == null) return new Credentials("", "");
        int colon = userInfo.indexOf(':');
        return new Credentials(decode(colon < 0 ? userInfo : userInfo.substring(0, colon)), colon < 0 ? "" : decode(userInfo.substring(colon + 1)));
    }
    private String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private record Credentials(String username, String password) {}
}
