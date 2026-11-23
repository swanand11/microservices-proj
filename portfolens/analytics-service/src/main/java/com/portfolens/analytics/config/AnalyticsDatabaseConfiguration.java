package com.portfolens.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsDatabaseConfiguration {
    @Bean
    DataSource analyticsDataSource(
            @Value("${ANALYTICS_DATABASE_URL:${DATABASE_URL:}}") String url,
            @Value("${DB_USER:${user:${DB_USERNAME:${DATABASE_USER:}}}}") String username,
            @Value("${DATABASE_PASSWORD:${DB_PASSWORD:}}") String password,
            @Value("${host:}") String host,
            @Value("${port:5432}") String port,
            @Value("${database:postgres}") String database) {
        HikariDataSource source = new HikariDataSource();
        if (url == null || url.isBlank()) {
            if (host == null || host.isBlank()) throw new IllegalStateException("Configure DATABASE_URL or Supabase host/port/database variables");
            source.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require");
        } else if (url.startsWith("jdbc:postgresql://")) {
            URI uri = URI.create(url.substring("jdbc:".length()));
            String query = uri.getRawQuery() == null ? "sslmode=require" : uri.getRawQuery();
            source.setJdbcUrl("jdbc:postgresql://" + hostPart(uri) + uri.getRawPath() + (query.contains("sslmode=") ? "?" + query : "?" + query + "&sslmode=require"));
            if (username.isBlank() && uri.getRawUserInfo() != null) username = decode(uri.getRawUserInfo().split(":", 2)[0]);
            if (password.isBlank() && uri.getRawUserInfo() != null && uri.getRawUserInfo().contains(":")) password = decode(uri.getRawUserInfo().split(":", 2)[1]);
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url);
            String query = uri.getRawQuery() == null ? "sslmode=require" : uri.getRawQuery();
            source.setJdbcUrl("jdbc:postgresql://" + hostPart(uri) + uri.getRawPath() + (query.contains("sslmode=") ? "?" + query : "?" + query + "&sslmode=require"));
            if (username.isBlank() && uri.getRawUserInfo() != null) username = decode(uri.getRawUserInfo().split(":", 2)[0]);
            if (password.isBlank() && uri.getRawUserInfo() != null && uri.getRawUserInfo().contains(":")) password = decode(uri.getRawUserInfo().split(":", 2)[1]);
        } else throw new IllegalStateException("Database URL must be a PostgreSQL URL");
        source.setUsername(username);
        source.setPassword(password);
        source.setDriverClassName("org.postgresql.Driver");
        source.setMaximumPoolSize(poolInt("ANALYTICS_DB_POOL_MAX_SIZE", 2));
        source.setMinimumIdle(poolInt("ANALYTICS_DB_POOL_MIN_IDLE", 1));
        source.setConnectionTimeout(poolLong("DB_POOL_CONNECTION_TIMEOUT_MS", 30_000));
        source.setIdleTimeout(poolLong("DB_POOL_IDLE_TIMEOUT_MS", 300_000));
        source.setMaxLifetime(poolLong("DB_POOL_MAX_LIFETIME_MS", 1_200_000));
        return source;
    }

    private static int poolInt(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        int parsed = Integer.parseInt(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be at least 1");
        return parsed;
    }

    private static long poolLong(String name, long fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        long parsed = Long.parseLong(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be at least 1");
        return parsed;
    }

    private static String hostPart(URI uri) {
        String authority = uri.getRawAuthority();
        return authority.substring(authority.lastIndexOf('@') + 1);
    }
    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
}
