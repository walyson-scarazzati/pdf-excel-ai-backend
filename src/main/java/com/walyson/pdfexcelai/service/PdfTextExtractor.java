package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Pattern;

@Service
public class PdfTextExtractor {

    private static final int MAX_RENDERED_PAGES = 3;
    private static final float IMAGE_DPI = 120f;
    private static final int PREVIEW_TEXT_LIMIT = 2000;
    private static final int MIN_TEXT_LENGTH_PER_PAGE = 24;
    private static final Pattern STRUCTURED_LINE_PATTERN = Pattern.compile(
            "(?im)^.*\\b\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?\\b.*(?:R?\\$?\\s*[+-]?\\d+[.,]\\d{2}).*$");
    private static final String[] STATEMENT_KEYWORDS = {
            "saldo", "pix", "pagamento", "boleto", "transfer", "debito", "credito", "tarifa",
            "saque", "cobranca", "rende", "contamax", "emprestimo"
    };

    private final OcrService ocrService;
    private final BankProfileResolver bankProfileResolver;

    public PdfTextExtractor(OcrService ocrService, BankProfileResolver bankProfileResolver) {
        this.ocrService = ocrService;
        this.bankProfileResolver = bankProfileResolver;
    }

    public PdfDocumentSnapshot extract(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (isImage(file)) {
            return extractFromImage(bytes, file.getOriginalFilename());
        }
        return extractFromPdf(bytes, file.getOriginalFilename());
    }

    private PdfDocumentSnapshot extractFromPdf(byte[] bytes, String fileName) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String rawText = extractDocumentText(document);
            List<String> pageTexts = extractPageTexts(document);
            BankProfile bankProfile = bankProfileResolver.resolve(fileName, rawText, pageTexts);
            OcrMode ocrMode = bankProfile.resolveOcrMode(ocrService.configuredMode());
            List<String> pageImages = new ArrayList<>();
            boolean shouldUseOcr = shouldRunOcr(pageTexts, document.getNumberOfPages(), bankProfile, ocrMode);
            boolean ocrUsed = false;

            PDFRenderer renderer = new PDFRenderer(document);
            int previewPageLimit = Math.min(document.getNumberOfPages(), MAX_RENDERED_PAGES);
            int ocrPageLimit = shouldUseOcr ? Math.min(document.getNumberOfPages(), ocrService.maxPages()) : 0;
            int renderLimit = Math.max(previewPageLimit, ocrPageLimit);

            for (int pageIndex = 0; pageIndex < renderLimit; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, IMAGE_DPI);
                if (pageIndex < previewPageLimit) {
                    pageImages.add(toDataUrl(image));
                }

                if (pageIndex < ocrPageLimit) {
                    String ocrText = normalizeText(ocrService.extractText(image, bankProfile));
                    if (StringUtils.hasText(ocrText)) {
                        ocrUsed = true;
                        pageTexts.set(pageIndex, mergePageText(pageTexts.get(pageIndex), ocrText, shouldUseOcr, ocrMode));
                    } else if (ocrMode == OcrMode.ONLY) {
                        pageTexts.set(pageIndex, "");
                    }
                }
            }

            String normalizedText = normalizeText(String.join("\n\n", pageTexts));
            if (!StringUtils.hasText(normalizedText)) {
                normalizedText = normalizeText(rawText);
            }

            String previewText = buildPreviewText(normalizedText, "Documento sem texto selecionavel. Ativa o OCR para tentar recuperar o conteudo.");
            return new PdfDocumentSnapshot(
                    rawText,
                    normalizedText,
                    previewText,
                    pageTexts,
                    pageImages,
                    document.getNumberOfPages(),
                    StringUtils.hasText(normalizedText),
                    ocrUsed,
                    "pdf",
                    bankProfile,
                    ocrMode
            );
        }
    }

    private PdfDocumentSnapshot extractFromImage(byte[] bytes, String fileName) throws IOException {
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("Formato de imagem nao suportado para OCR");
        }

        BankProfile bankProfile = bankProfileResolver.resolve(fileName, "", List.of());
        OcrMode ocrMode = bankProfile.resolveOcrMode(ocrService.configuredMode());
        String ocrText = normalizeText(ocrService.extractText(image, bankProfile));
        List<String> pageImages = List.of(toDataUrl(image));
        List<String> pageTexts = List.of(ocrText);
        String previewText = buildPreviewText(ocrText, "Imagem sem texto identificado. Ativa e instala o OCR local para processar este tipo de ficheiro.");

        return new PdfDocumentSnapshot(
                ocrText,
                ocrText,
                previewText,
                pageTexts,
                pageImages,
                1,
                StringUtils.hasText(ocrText),
                StringUtils.hasText(ocrText),
                "image",
                bankProfile,
                ocrMode
        );
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        return (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/"))
                || (StringUtils.hasText(fileName) && fileName.toLowerCase().matches(".*\\.(png|jpg|jpeg|webp|bmp|tif|tiff)$"));
    }

    private boolean shouldRunOcr(List<String> pageTexts, int pageCount, BankProfile bankProfile, OcrMode ocrMode) {
        if (!ocrService.isEnabled()) {
            return false;
        }

        if (ocrMode == OcrMode.FIRST || ocrMode == OcrMode.ONLY) {
            return true;
        }

        if (pageTexts.isEmpty()) {
            return true;
        }

        int minExpectedLength = Math.max(pageCount, 1) * MIN_TEXT_LENGTH_PER_PAGE;
        int currentLength = String.join("", pageTexts).trim().length();
        if (currentLength < minExpectedLength) {
            return true;
        }

        int structuredLines = pageTexts.stream().mapToInt(this::scoreStructuredText).sum();
        int minStructuredLines = Math.max(2, Math.min(pageCount, ocrService.maxPages()));
        if (structuredLines < minStructuredLines) {
            return true;
        }

        return bankProfile == BankProfile.BANCO_DO_BRASIL && structuredLines < minStructuredLines * 4;
    }

    private String mergePageText(String extractedText, String ocrText, boolean shouldUseOcr, OcrMode ocrMode) {
        if (!StringUtils.hasText(extractedText)) {
            return ocrText;
        }
        if (!StringUtils.hasText(ocrText) || extractedText.contains(ocrText)) {
            return ocrMode == OcrMode.ONLY ? "" : extractedText;
        }

        if (ocrMode == OcrMode.ONLY || ocrMode == OcrMode.FIRST) {
            return ocrText;
        }

        int extractedScore = scoreStructuredText(extractedText);
        int ocrScore = scoreStructuredText(ocrText);
        if (ocrScore > extractedScore) {
            return ocrText;
        }

        return extractedText + "\n" + ocrText;
    }

    private int scoreStructuredText(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }

        int score = countMatches(STRUCTURED_LINE_PATTERN, text);
        if (score > 0) {
            return score;
        }

        String normalized = normalizeForComparison(text);
        for (String keyword : STATEMENT_KEYWORDS) {
            if (normalized.contains(keyword)) {
                score++;
            }
        }

        return score / 2;
    }

    private int countMatches(Pattern pattern, String text) {
        int count = 0;
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String normalizeForComparison(String value) {
        String normalized = value.toLowerCase();
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private String extractDocumentText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setAddMoreFormatting(true);
        stripper.setLineSeparator("\n");
        stripper.setWordSeparator("  ");
        return stripper.getText(document).trim();
    }

    private List<String> extractPageTexts(PDDocument document) throws IOException {
        List<String> pageTexts = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            PDFTextStripper pageStripper = new PDFTextStripper();
            pageStripper.setSortByPosition(true);
            pageStripper.setAddMoreFormatting(true);
            pageStripper.setLineSeparator("\n");
            pageStripper.setWordSeparator("  ");
            pageStripper.setStartPage(pageIndex + 1);
            pageStripper.setEndPage(pageIndex + 1);
            pageTexts.add(normalizeText(pageStripper.getText(document)));
        }
        return pageTexts;
    }

    private String buildPreviewText(String normalizedText, String fallbackMessage) {
        if (!StringUtils.hasText(normalizedText)) {
            return fallbackMessage;
        }
        return normalizedText.substring(0, Math.min(normalizedText.length(), PREVIEW_TEXT_LIMIT));
    }

    private String toDataUrl(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\f\\u000B]+", "    ")
                .replaceAll(" {3,}", "  ")
                .replaceAll("\\n{3,}", "\\n\\n");

        String[] lines = normalized.split("\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String cleanedLine = line.stripTrailing();
            if (cleanedLine.isBlank()) {
                if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
                    builder.append("\n");
                }
                builder.append("\n");
                continue;
            }

            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
                builder.append("\n");
            }
            builder.append(cleanedLine.trim());
        }

        return builder.toString().trim();
    }
}
