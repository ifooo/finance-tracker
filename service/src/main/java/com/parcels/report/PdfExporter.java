package com.parcels.report;

import com.parcels.report.dto.MonthlyReport;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.BaseFont;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a {@link MonthlyReport} to {@code <outbox>/<yyyy-MM>-report.pdf}: FreeMarker builds XHTML,
 * Flying Saucer lays it out as A4 pages. Liberation Sans is embedded, so Cyrillic text (e.g. merchant names) renders.
 */
@Component
@RequiredArgsConstructor
public class PdfExporter {

    static final String TEMPLATE = "monthly-report-pdf.ftl";
    static final String FONT_FAMILY = "Liberation Sans";
    private static final List<String> FONTS = List.of(
            "liberation/LiberationSans-Regular.ttf",
            "liberation/LiberationSans-Bold.ttf",
            "liberation/LiberationSans-Italic.ttf",
            "liberation/LiberationSans-BoldItalic.ttf");

    private final Configuration freemarker;

    public Path export(MonthlyReport report, Path outboxDir) {
        Objects.requireNonNull(outboxDir, "pft.reports.outbox-dir is not configured");
        Path target = outboxDir.resolve(report.month() + "-report.pdf");
        try {
            Files.createDirectories(outboxDir);
            Path temp = Files.createTempFile(outboxDir, ".report-", ".pdf.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                write(renderHtml(report), out);
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(temp);
                throw e;
            }
            return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }

    // package-private for tests
    String renderHtml(MonthlyReport report) {
        StringWriter out = new StringWriter();
        try {
            freemarker.getTemplate(TEMPLATE).process(Map.of("report", report, "fontFamily", FONT_FAMILY), out);
        } catch (TemplateException | IOException e) {
            throw new IllegalStateException("Could not render " + TEMPLATE, e);
        }
        return out.toString();
    }

    private static void write(String xhtml, OutputStream out) throws IOException {
        ITextRenderer renderer = new ITextRenderer();
        registerFonts(renderer.getFontResolver());
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (DocumentException e) {
            throw new IOException("Could not create PDF", e);
        }
    }

    /** OpenPDF resolves these paths as classpath resources (openpdf-fonts-extra). The family name comes from the font. */
    private static void registerFonts(ITextFontResolver resolver) throws IOException {
        for (String resource : FONTS) {
            try {
                resolver.addFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (DocumentException e) {
                throw new IOException("Could not load font " + resource, e);
            }
        }
    }
}
