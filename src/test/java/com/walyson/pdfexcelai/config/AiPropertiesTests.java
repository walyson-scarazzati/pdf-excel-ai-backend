package com.walyson.pdfexcelai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTests {

    @Test
    void aiProperties_canBeCreated() {
        AiProperties props = new AiProperties(
                "github-models",
                "https://api.github.com/models",
                "ghp_test123",
                "gpt-4o",
                "2024-02-01"
        );

        assertThat(props.provider()).isEqualTo("github-models");
        assertThat(props.apiUrl()).isEqualTo("https://api.github.com/models");
        assertThat(props.apiKey()).isEqualTo("ghp_test123");
        assertThat(props.model()).isEqualTo("gpt-4o");
        assertThat(props.githubApiVersion()).isEqualTo("2024-02-01");
    }

    @Test
    void aiProperties_recordContract() {
        AiProperties props1 = new AiProperties("provider", "url", "key", "model", "version");
        AiProperties props2 = new AiProperties("provider", "url", "key", "model", "version");

        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
    }

    @Test
    void aiProperties_withNullFields() {
        AiProperties props = new AiProperties(null, null, null, null, null);

        assertThat(props.provider()).isNull();
        assertThat(props.apiUrl()).isNull();
        assertThat(props.apiKey()).isNull();
    }

    @Test
    void aiProperties_withEmptyStrings() {
        AiProperties props = new AiProperties("", "", "", "", "");

        assertThat(props.provider()).isEmpty();
        assertThat(props.apiUrl()).isEmpty();
        assertThat(props.apiKey()).isEmpty();
    }

    @Test
    void aiProperties_toString() {
        AiProperties props = new AiProperties("github", "url", "key", "model", "version");

        assertThat(props.toString()).contains("github", "url", "model");
    }

    @Test
    void aiProperties_differentProviders() {
        AiProperties githubProps = new AiProperties("github-models", "url", "key", "model", "version");
        AiProperties azureProps = new AiProperties("azure", "url", "key", "model", "version");

        assertThat(githubProps.provider()).isNotEqualTo(azureProps.provider());
    }
}
