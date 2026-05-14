package com.walyson.pdfexcelai.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";
    private static final int ORDER_AFTER_CONFIG_DATA = Ordered.HIGHEST_PRECEDENCE + 20;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configuredUrl = trimToNull(environment.getProperty("SPRING_DATASOURCE_URL"));
        if (configuredUrl == null) {
            configuredUrl = environment.getProperty("DB_URL");
        }

        NormalizedDatabaseUrl normalized = normalizeDatabaseUrlInternal(
                configuredUrl,
                environment.getProperty("DB_HOST"),
                environment.getProperty("DB_PORT"),
                environment.getProperty("DB_NAME"));

        if (normalized == null) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", normalized.url());
        if (normalized.username() != null) {
            properties.put("spring.datasource.username", normalized.username());
        }
        if (normalized.password() != null) {
            properties.put("spring.datasource.password", normalized.password());
        }
        environment.getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    static String normalizeDatabaseUrl(String dbUrl, String dbHost, String dbPort, String dbName) {
        NormalizedDatabaseUrl normalized = normalizeDatabaseUrlWithCredentials(dbUrl, dbHost, dbPort, dbName);
        return normalized == null ? null : normalized.url();
    }

    private static NormalizedDatabaseUrl normalizeDatabaseUrlInternal(String dbUrl, String dbHost, String dbPort, String dbName) {
        return normalizeDatabaseUrlWithCredentials(dbUrl, dbHost, dbPort, dbName);
    }

    private static NormalizedDatabaseUrl normalizeDatabaseUrlWithCredentials(String dbUrl, String dbHost, String dbPort, String dbName) {
        String value = trimToNull(dbUrl);
        if (value == null) {
            return null;
        }

        String poolerHost = trimToNull(dbHost);
        if (isPoolerHost(poolerHost) && isDirectSupabaseUrl(value)) {
            return new NormalizedDatabaseUrl(buildJdbcUrl(poolerHost, dbPort, dbName, true), null, null);
        }

        if (value.startsWith("jdbc:")) {
            return normalizeUriStyleUrl(value.substring("jdbc:".length()));
        }

        if (value.startsWith("postgresql://")) {
            return normalizeUriStyleUrl(value);
        }

        if (value.startsWith("postgres://")) {
            return normalizeUriStyleUrl("postgresql://" + value.substring("postgres://".length()));
        }

        String host = trimToNull(dbHost);
        if (host == null) {
            host = value;
        }

        if (value.contains("/") || value.contains("?")) {
            return new NormalizedDatabaseUrl("jdbc:postgresql://" + value, null, null);
        }

        return new NormalizedDatabaseUrl(buildJdbcUrl(host, dbPort, dbName, isSupabaseHost(host)), null, null);
    }

    private static NormalizedDatabaseUrl normalizeUriStyleUrl(String value) {
        URI uri = URI.create(value);
        if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
            return new NormalizedDatabaseUrl("jdbc:" + value, null, null);
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://");
        jdbcUrl.append(uri.getHost());
        if (uri.getPort() > -1) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            jdbcUrl.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int separator = userInfo.indexOf(':');
            if (separator >= 0) {
                username = decode(userInfo.substring(0, separator));
                password = decode(userInfo.substring(separator + 1));
            } else {
                username = decode(userInfo);
            }
        }

        return new NormalizedDatabaseUrl(jdbcUrl.toString(), username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String buildJdbcUrl(String host, String dbPort, String dbName, boolean requireSsl) {
        String port = trimToNull(dbPort);
        if (port == null) {
            port = "5432";
        }

        String database = trimToNull(dbName);
        if (database == null) {
            database = isSupabaseHost(host) || isPoolerHost(host) ? "postgres" : "pdf_excel_ai";
        }

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        if (requireSsl) {
            url += "?sslmode=require";
        }
        return url;
    }

    private static boolean isSupabaseHost(String host) {
        return host != null && host.endsWith(".supabase.co");
    }

    private static boolean isPoolerHost(String host) {
        return host != null && host.endsWith(".pooler.supabase.com");
    }

    private static boolean isDirectSupabaseUrl(String value) {
        return value.contains(".supabase.co")
                && !value.contains(".pooler.supabase.com");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public int getOrder() {
        return ORDER_AFTER_CONFIG_DATA;
    }

    private record NormalizedDatabaseUrl(String url, String username, String password) {
    }
}
