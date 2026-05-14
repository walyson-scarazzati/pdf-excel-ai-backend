package com.walyson.pdfexcelai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTests {

    @Test
    void keepsJdbcUrlUnchanged() {
        String url = "jdbc:postgresql://localhost:5432/pdf_excel_ai";

        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(url, null, null, null))
                .isEqualTo(url);
    }

    @Test
    void convertsPostgresUriToJdbcUrl() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "postgresql://localhost:5432/pdf_excel_ai",
                null,
                null,
                null))
                .isEqualTo("jdbc:postgresql://localhost:5432/pdf_excel_ai");
    }

    @Test
    void removesCredentialsFromRenderStylePostgresUri() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "postgresql://app_user:secret@dpg-example-a.virginia-postgres.render.com/app_db",
                null,
                null,
                null))
                .isEqualTo("jdbc:postgresql://dpg-example-a.virginia-postgres.render.com/app_db");
    }

    @Test
    void removesCredentialsFromJdbcUri() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "jdbc:postgresql://app_user:secret@dpg-example-a.virginia-postgres.render.com/app_db",
                null,
                null,
                null))
                .isEqualTo("jdbc:postgresql://dpg-example-a.virginia-postgres.render.com/app_db");
    }

    @Test
    void expandsSupabaseHostOnlyUrl() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "db.hwtbulztousmqgllwmrh.supabase.co",
                null,
                null,
                null))
                .isEqualTo("jdbc:postgresql://db.hwtbulztousmqgllwmrh.supabase.co:5432/postgres?sslmode=require");
    }

    @Test
    void usesConfiguredHostPartsWhenDbUrlIsHostOnly() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "db.example.com",
                "db.internal",
                "6543",
                "app_db"))
                .isEqualTo("jdbc:postgresql://db.internal:6543/app_db");
    }

    @Test
    void replacesDirectSupabaseJdbcUrlWithConfiguredPoolerHost() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.normalizeDatabaseUrl(
                "jdbc:postgresql://db.hwtbulztousmqgllwmrh.supabase.co:5432/postgres?sslmode=require",
                "aws-1-eu-central-1.pooler.supabase.com",
                "5432",
                "postgres"))
                .isEqualTo("jdbc:postgresql://aws-1-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require");
    }
}
