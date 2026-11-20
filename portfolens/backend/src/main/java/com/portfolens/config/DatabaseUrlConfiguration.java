package com.portfolens.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Converts Supabase postgres:// URIs into JDBC URL + separate credentials for Hikari. */
@Configuration
public class DatabaseUrlConfiguration {
    private final String databaseUrl;
    private final String splitHost;
    private final String splitPort;
    private final String splitDatabase;
    private final String configuredUsername;
    private final String configuredPassword;

    public DatabaseUrlConfiguration(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${host:}") String splitHost,
            @Value("${port:5432}") String splitPort,
            @Value("${database:postgres}") String splitDatabase,
            @Value("${DB_USER:${user:${DB_USERNAME:${DATABASE_USER:}}}}") String configuredUsername,
            @Value("${DATABASE_PASSWORD:${DB_PASSWORD:}}") String configuredPassword) {
        this.databaseUrl = databaseUrl;
        this.splitHost = splitHost;
        this.splitPort = splitPort;
        this.splitDatabase = splitDatabase;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @Bean
    DataSource portfolensDataSource() {
        ParsedConnection parsed = parseConnection();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(parsed.jdbcUrl());
        dataSource.setUsername(firstNonBlank(configuredUsername, parsed.username()));
        dataSource.setPassword(firstNonBlank(configuredPassword, parsed.password()));
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    private ParsedConnection parseConnection() {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            if (splitHost == null || splitHost.isBlank()) {
                throw new IllegalStateException("Configure DATABASE_URL or Supabase host/port/database environment variables");
            }
            String db = splitDatabase == null || splitDatabase.isBlank() ? "postgres" : splitDatabase;
            String port = splitPort == null || splitPort.isBlank() ? "5432" : splitPort;
            return new ParsedConnection("jdbc:postgresql://" + splitHost + ":" + port + "/" + db + "?sslmode=require", "", "");
        }

        String value = databaseUrl.trim();
        if (value.startsWith("jdbc:postgresql://")) {
            return normalizeJdbcUri(value);
        }
        if (value.startsWith("postgresql://") || value.startsWith("postgres://")) {
            return normalizePostgresUri(value);
        }
        throw new IllegalStateException("DATABASE_URL must use jdbc:postgresql://, postgresql://, or postgres://");
    }

    private ParsedConnection normalizePostgresUri(String value) {
        URI uri = URI.create(value);
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() < 2) {
            throw new IllegalStateException("DATABASE_URL must include a PostgreSQL host and database name");
        }
        Credentials credentials = parseUserInfo(uri.getRawUserInfo());
        String query = mergeSsl(uri.getRawQuery());
        String jdbc = "jdbc:postgresql://" + uri.getRawAuthority().substring(uri.getRawAuthority().lastIndexOf('@') + 1)
                + uri.getRawPath() + "?" + query;
        return new ParsedConnection(jdbc, credentials.username(), credentials.password());
    }

    private ParsedConnection normalizeJdbcUri(String value) {
        URI uri = URI.create(value.substring("jdbc:".length()));
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() < 2) {
            throw new IllegalStateException("DATABASE_URL must include a PostgreSQL host and database name");
        }
        Credentials credentials = parseUserInfo(uri.getRawUserInfo());
        String query = mergeSsl(uri.getRawQuery());
        String jdbc = "jdbc:postgresql://" + uri.getRawAuthority().substring(uri.getRawAuthority().lastIndexOf('@') + 1)
                + uri.getRawPath() + "?" + query;
        return new ParsedConnection(jdbc, credentials.username(), credentials.password());
    }

    private Credentials parseUserInfo(String rawUserInfo) {
        if (rawUserInfo == null || rawUserInfo.isBlank()) return new Credentials("", "");
        int separator = rawUserInfo.indexOf(':');
        String user = separator < 0 ? rawUserInfo : rawUserInfo.substring(0, separator);
        String password = separator < 0 ? "" : rawUserInfo.substring(separator + 1);
        return new Credentials(decode(user), decode(password));
    }

    private String mergeSsl(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                String[] parts = pair.split("=", 2);
                params.put(decode(parts[0]), parts.length == 2 ? parts[1] : "");
            }
        }
        params.putIfAbsent("sslmode", "require");
        return params.entrySet().stream().map(e -> e.getKey() + (e.getValue().isEmpty() ? "" : "=" + e.getValue()))
                .reduce((a, b) -> a + "&" + b).orElse("sslmode=require");
    }

    private String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private String firstNonBlank(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
    private record Credentials(String username, String password) {}
    private record ParsedConnection(String jdbcUrl, String username, String password) {}
}
