package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.config.OcrProperties;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OcrService {

    private final OcrProperties ocrProperties;

    public OcrService(OcrProperties ocrProperties) {
        this.ocrProperties = ocrProperties;
    }

    public boolean isEnabled() {
        return ocrProperties.enabled() && StringUtils.hasText(ocrProperties.command());
    }

    public boolean isAvailable() {
        if (!isEnabled()) {
            return false;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(versionCommand()).redirectErrorStream(true).start();
            boolean completed = process.waitFor(Math.max(3, ocrProperties.timeoutSeconds()), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String commandName() {
        return defaultIfBlank(ocrProperties.command(), "tesseract");
    }

    public int maxPages() {
        return Math.max(1, ocrProperties.maxPages());
    }

    public OcrMode configuredMode() {
        return OcrMode.from(ocrProperties.mode());
    }

    public String extractText(BufferedImage image) {
        return extractText(image, BankProfile.UNKNOWN);
    }

    public String extractText(BufferedImage image, BankProfile bankProfile) {
        if (!isEnabled() || image == null || !isAvailable()) {
            return "";
        }

        if (bankProfile == BankProfile.BANCO_DO_BRASIL) {
            String columnText = extractBancoDoBrasilColumnText(image);
            if (StringUtils.hasText(columnText)) {
                return columnText;
            }
        }

        List<String> fragments = new ArrayList<>();
        for (BufferedImage region : buildRegions(image, bankProfile)) {
            String text = runTesseract(region, bankProfile);
            if (StringUtils.hasText(text)) {
                fragments.add(text);
            }
        }

        return mergeUniqueLines(fragments);
    }

    private String runTesseract(BufferedImage image, BankProfile bankProfile) {
        BufferedImage processedImage = preprocess(image, bankProfile);

        Path tempInputFile = null;
        try {
            tempInputFile = Files.createTempFile("pdf-excel-ai-ocr-", ".png");
            ImageIO.write(processedImage, "png", tempInputFile.toFile());

            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(tempInputFile));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean completed = process.waitFor(Math.max(5, ocrProperties.timeoutSeconds()), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "";
            }

            if (process.exitValue() != 0) {
                return "";
            }

            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        } catch (IOException ex) {
            return "";
        } finally {
            if (tempInputFile != null) {
                try {
                    Files.deleteIfExists(tempInputFile);
                } catch (IOException ignored) {
                    // Temporary OCR file cleanup failure is non-fatal.
                }
            }
        }
    }

    private List<BufferedImage> buildRegions(BufferedImage image, BankProfile bankProfile) {
        List<BufferedImage> regions = new ArrayList<>();
        regions.add(image);

        BufferedImage body = cropBodyRegion(image, bankProfile);
        if (body != image) {
            regions.add(body);
            int midpoint = Math.max(1, body.getHeight() / 2);
            regions.add(body.getSubimage(0, 0, body.getWidth(), midpoint));
            regions.add(body.getSubimage(0, Math.max(0, midpoint - 1), body.getWidth(), body.getHeight() - Math.max(0, midpoint - 1)));
        }

        return regions;
    }

    private BufferedImage preprocess(BufferedImage image, BankProfile bankProfile) {
        if (!ocrProperties.preprocessEnabled()) {
            return image;
        }

        BufferedImage working = image;
        if (bankProfile == BankProfile.BANCO_DO_BRASIL) {
            working = upscale(working);
        }

        BufferedImage processed = new BufferedImage(working.getWidth(), working.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = processed.createGraphics();
        graphics.drawImage(working, 0, 0, null);
        graphics.dispose();

        double contrastFactor = Math.max(1.0d, ocrProperties.contrastFactor());
        int threshold = Math.max(0, Math.min(255, ocrProperties.threshold()));
        for (int y = 0; y < processed.getHeight(); y++) {
            for (int x = 0; x < processed.getWidth(); x++) {
                int rgb = processed.getRGB(x, y);
                int gray = rgb & 0xFF;
                int contrasted = (int) Math.max(0, Math.min(255, ((gray - 128) * contrastFactor) + 128));
                int binary = contrasted >= threshold ? 255 : 0;
                int output = new Color(binary, binary, binary).getRGB();
                processed.setRGB(x, y, output);
            }
        }

        if (bankProfile == BankProfile.BANCO_DO_BRASIL) {
            processed = removeNoise(processed);
            if (ocrProperties.deskewEnabled()) {
                processed = deskew(processed);
            }
        }

        return processed;
    }

    private BufferedImage upscale(BufferedImage image) {
        int factor = Math.max(1, ocrProperties.upscaleFactor());
        if (factor == 1) {
            return image;
        }

        BufferedImage scaled = new BufferedImage(image.getWidth() * factor, image.getHeight() * factor, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        graphics.dispose();
        return scaled;
    }

    private BufferedImage removeNoise(BufferedImage image) {
        BufferedImage denoised = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 1; y < image.getHeight() - 1; y++) {
            for (int x = 1; x < image.getWidth() - 1; x++) {
                int darkNeighbors = 0;
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetX = -1; offsetX <= 1; offsetX++) {
                        int value = image.getRGB(x + offsetX, y + offsetY) & 0xFF;
                        if (value < 128) {
                            darkNeighbors++;
                        }
                    }
                }
                int binary = darkNeighbors >= 4 ? 0 : 255;
                denoised.setRGB(x, y, new Color(binary, binary, binary).getRGB());
            }
        }
        return denoised;
    }

    private BufferedImage deskew(BufferedImage image) {
        double bestAngle = 0d;
        double bestScore = Double.NEGATIVE_INFINITY;
        double maxAngle = Math.max(0d, ocrProperties.maxDeskewAngle());
        for (double angle = -maxAngle; angle <= maxAngle; angle += 0.5d) {
            double score = scoreHorizontalAlignment(image, angle);
            if (score > bestScore) {
                bestScore = score;
                bestAngle = angle;
            }
        }
        return Math.abs(bestAngle) < 0.01d ? image : rotate(image, bestAngle);
    }

    private double scoreHorizontalAlignment(BufferedImage image, double angleDegrees) {
        BufferedImage rotated = rotate(image, angleDegrees);
        double score = 0d;
        for (int y = 0; y < rotated.getHeight(); y++) {
            int darkPixels = 0;
            for (int x = 0; x < rotated.getWidth(); x++) {
                if ((rotated.getRGB(x, y) & 0xFF) < 128) {
                    darkPixels++;
                }
            }
            score += darkPixels * darkPixels;
        }
        return score;
    }

    private BufferedImage rotate(BufferedImage image, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        AffineTransform transform = new AffineTransform();
        transform.rotate(radians, image.getWidth() / 2.0d, image.getHeight() / 2.0d);
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotated = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        operation.filter(image, rotated);
        return rotated;
    }

    private String extractBancoDoBrasilColumnText(BufferedImage image) {
        BufferedImage body = cropBodyRegion(image, BankProfile.BANCO_DO_BRASIL);
        BufferedImage processedBody = preprocess(body, BankProfile.BANCO_DO_BRASIL);
        List<BufferedImage> columns = List.of(
                cropColumn(processedBody, 0.00d, 0.12d),
                cropColumn(processedBody, 0.12d, 0.21d),
                cropColumn(processedBody, 0.21d, 0.31d),
                cropColumn(processedBody, 0.31d, 0.78d),
                cropColumn(processedBody, 0.78d, 0.95d),
                cropColumn(processedBody, 0.95d, 1.00d)
        );

        List<List<String>> linesByColumn = new ArrayList<>();
        for (BufferedImage column : columns) {
            String text = runTesseractWithoutPreprocess(column);
            linesByColumn.add(splitUsefulLines(text));
        }

        int rowCount = linesByColumn.stream().mapToInt(List::size).max().orElse(0);
        if (rowCount == 0) {
            return "";
        }

        List<String> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            StringBuilder builder = new StringBuilder("COL|");
            for (int columnIndex = 0; columnIndex < linesByColumn.size(); columnIndex++) {
                String value = rowIndex < linesByColumn.get(columnIndex).size() ? linesByColumn.get(columnIndex).get(rowIndex) : "";
                if (columnIndex > 0) {
                    builder.append('|');
                }
                builder.append(value);
            }
            rows.add(builder.toString());
        }

        return String.join("\n", rows);
    }

    private BufferedImage cropColumn(BufferedImage image, double startRatio, double endRatio) {
        int startX = Math.max(0, Math.min(image.getWidth() - 1, (int) Math.round(image.getWidth() * startRatio)));
        int endX = Math.max(startX + 1, Math.min(image.getWidth(), (int) Math.round(image.getWidth() * endRatio)));
        return image.getSubimage(startX, 0, endX - startX, image.getHeight());
    }

    private String runTesseractWithoutPreprocess(BufferedImage image) {
        Path tempInputFile = null;
        try {
            tempInputFile = Files.createTempFile("pdf-excel-ai-ocr-col-", ".png");
            ImageIO.write(image, "png", tempInputFile.toFile());
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(tempInputFile));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean completed = process.waitFor(Math.max(5, ocrProperties.timeoutSeconds()), TimeUnit.SECONDS);
            if (!completed || process.exitValue() != 0) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "";
        } finally {
            if (tempInputFile != null) {
                try {
                    Files.deleteIfExists(tempInputFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<String> splitUsefulLines(String text) {
        List<String> lines = new ArrayList<>();
        Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> line.length() > 1)
                .forEach(lines::add);
        return lines;
    }

    private BufferedImage cropBodyRegion(BufferedImage image, BankProfile bankProfile) {
        double topRatio = Math.max(0d, ocrProperties.cropTopRatio());
        double bottomRatio = Math.max(0d, ocrProperties.cropBottomRatio());
        double sideRatio = Math.max(0d, ocrProperties.cropSideRatio());

        if (bankProfile == BankProfile.BANCO_DO_BRASIL) {
            topRatio = Math.max(topRatio, 0.18d);
        } else if (bankProfile == BankProfile.SANTANDER) {
            topRatio = Math.max(topRatio, 0.14d);
        }

        int top = Math.min(image.getHeight() - 1, (int) Math.round(image.getHeight() * topRatio));
        int bottom = Math.min(image.getHeight() - top - 1, (int) Math.round(image.getHeight() * bottomRatio));
        int side = Math.min(image.getWidth() / 4, (int) Math.round(image.getWidth() * sideRatio));
        int width = Math.max(1, image.getWidth() - (side * 2));
        int height = Math.max(1, image.getHeight() - top - bottom);
        if (width >= image.getWidth() && height >= image.getHeight()) {
            return image;
        }
        return image.getSubimage(side, top, width, height);
    }

    private String mergeUniqueLines(List<String> fragments) {
        Set<String> uniqueLines = new LinkedHashSet<>();
        for (String fragment : fragments) {
            Arrays.stream(fragment.split("\\R"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(uniqueLines::add);
        }
        return String.join("\n", uniqueLines);
    }

    private List<String> buildCommand(Path inputFile) {
        return List.of(
                ocrProperties.command(),
                inputFile.toAbsolutePath().toString(),
                "stdout",
                "-l",
                resolveLanguage(),
                "--psm",
                Integer.toString(Math.max(1, ocrProperties.pageSegmentationMode()))
        );
    }

    private String resolveLanguage() {
        String configured = defaultIfBlank(ocrProperties.language(), "por");
        Set<String> languages = new LinkedHashSet<>();
        languages.add("por");

        Arrays.stream(configured.split("\\+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(language -> language.toLowerCase(Locale.ROOT))
                .forEach(languages::add);

        return String.join("+", languages);
    }

    private List<String> versionCommand() {
        List<String> command = new ArrayList<>();
        command.add(commandName());
        command.add("--version");
        return command;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}