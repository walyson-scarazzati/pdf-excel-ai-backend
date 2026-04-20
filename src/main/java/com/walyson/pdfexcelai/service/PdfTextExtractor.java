package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractor {

    private static final int MAX_RENDERED_PAGES = 3;
    private static final float IMAGE_DPI = 144f;

    public PdfDocumentSnapshot extract(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document).trim();
            PDFRenderer renderer = new PDFRenderer(document);
            List<String> pageImages = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < Math.min(document.getNumberOfPages(), MAX_RENDERED_PAGES); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, IMAGE_DPI);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", outputStream);
                    String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                    pageImages.add("data:image/png;base64," + base64);
                }
            }

            String previewText = rawText.isBlank()
                    ? "PDF sem texto selecionavel. A IA vai depender das imagens geradas das paginas."
                    : rawText.substring(0, Math.min(rawText.length(), 1200));

            return new PdfDocumentSnapshot(
                    rawText,
                    previewText,
                    pageImages,
                    document.getNumberOfPages(),
                    !rawText.isBlank()
            );
        }
    }
}