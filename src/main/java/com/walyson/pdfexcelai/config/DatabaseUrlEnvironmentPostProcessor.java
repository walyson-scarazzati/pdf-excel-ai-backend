package com.walyson.pdfexcelai.config;

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
        if (trimToNull(environment.getProperty("SPRING_DATASOURCE_URL")) != null) {
            return;
        }

        String normalizedUrl = normalizeDatabaseUrl(
                environment.getProperty("DB_URL"),
                environment.getProperty("DB_HOST"),
                environment.getProperty("DB_PORT"),
                environment.getProperty("DB_NAME"));

        if (normalizedUrl == null) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", normalizedUrl);
        environment.getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    static String normalizeDatabaseUrl(String dbUrl, String dbHost, String dbPort, String dbName) {
        String value = trimToNull(dbUrl);
        if (value == null) {
            return null;
        }

        if (value.startsWith("jdbc:")) {
            return value;
        }

        if (value.startsWith("postgresql://")) {
            return "jdbc:" + value;
        }

        if (value.startsWith("postgres://")) {
            return "jdbc:postgresql://" + value.substring("postgres://".length());
        }

        String host = trimToNull(dbHost);
        if (host == null) {
            host = value;
        }

        String port = trimToNull(dbPort);
        if (port == null) {
            port = "5432";
        }

        String database = trimToNull(dbName);
        if (database == null) {
            database = isSupabaseHost(host) ? "postgres" : "pdf_excel_ai";
        }

        if (value.contains("/") || value.contains("?")) {
            return "jdbc:postgresql://" + value;
        }

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        if (isSupabaseHost(host)) {
            url += "?sslmode=require";
        }
        return url;
    }

    private static boolean isSupabaseHost(String host) {
        return host != null && host.endsWith(".supabase.co");
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
}
