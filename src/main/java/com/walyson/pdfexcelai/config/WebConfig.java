package com.walyson.pdfexcelai.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({CorsProperties.class, AiProperties.class, OcrProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private static final Set<String> DEFAULT_ALLOWED_ORIGINS = Set.of(
            "http://localhost:4200",
            "https://pdf-excel-ai-frontend.vercel.app"
    );

    private static final Set<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = Set.of(
            "https://*.run.app"
    );

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        LinkedHashSet<String> allowedOrigins = new LinkedHashSet<>(DEFAULT_ALLOWED_ORIGINS);
        if (corsProperties.allowedOrigins() != null) {
            allowedOrigins.addAll(corsProperties.allowedOrigins());
        }

        LinkedHashSet<String> allowedOriginPatterns = new LinkedHashSet<>(DEFAULT_ALLOWED_ORIGIN_PATTERNS);
        if (corsProperties.allowedOriginPatterns() != null) {
            allowedOriginPatterns.addAll(corsProperties.allowedOriginPatterns());
        }

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
