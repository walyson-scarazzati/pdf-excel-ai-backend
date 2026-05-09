package com.walyson.pdfexcelai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrProperties(
        boolean enabled,
        String command,
        String mode,
        String language,
        int pageSegmentationMode,
        int timeoutSeconds,
        int maxPages,
        boolean preprocessEnabled,
        double contrastFactor,
        int threshold,
        int upscaleFactor,
        boolean deskewEnabled,
        double maxDeskewAngle,
        double cropTopRatio,
        double cropBottomRatio,
        double cropSideRatio
) {
}