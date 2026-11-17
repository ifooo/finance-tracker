package com.parcels.report;

import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.report.dto.MonthlyReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfExporterTest extends BaseDbTest {

    @TempDir
    Path outbox;

    @Autowired
    private PdfExporter pdfExporter;

    @Test
    void householdReportHasOverviewPerPersonSplitAndTransactions() throws IOException {
        Path file = pdfExporter.export(ReportFixtures.sampleReport(), outbox);

        assertThat(file.getFileName()).hasToString("2026-10-report.pdf");
        try (PdfReader reader = new PdfReader(file.toString())) {
            int pages = reader.getNumberOfPages();
            assertThat(pages).isGreaterThanOrEqualTo(2); // transactions start on a new page
            String overview = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(overview)
                    .contains("October 2026")
                    .contains("Ivo Test · Ana Test")
                    .contains("58,350.00")
                    .contains("Per person")
                    .contains("-400.00")
                    .contains("Expenses by category")
                    .contains("Top merchants")
                    .contains("Page 1 of " + pages);
            String transactions = new PdfTextExtractor(reader).getTextFromPage(pages);
            assertThat(transactions).contains("Transactions").contains("PERSON").contains("Ana Trattoria").contains("−18.50 EUR");
        }
    }

    @Test
    void singlePersonReportLeavesOutThePerPersonSections() throws IOException {
        Path file = pdfExporter.export(ReportFixtures.singlePersonReport(), outbox);

        try (PdfReader reader = new PdfReader(file.toString())) {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page));
            }
            assertThat(text).doesNotContain("Per person").doesNotContain("PERSON");
        }
    }

    @Test
    void embedsAFontThatRendersCyrillic() throws IOException {
        Path file = pdfExporter.export(ReportFixtures.sampleReport(), outbox);

        try (PdfReader reader = new PdfReader(file.toString())) {
            assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("Веро Центар");
            PdfDictionary fonts = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.FONT);
            assertThat(fonts.getKeys()).extracting(key -> fonts.getAsDict(key).getAsName(PdfName.BASEFONT).toString())
                    .isNotEmpty()
                    .allMatch(name -> name.contains("LiberationSans"));
        }
    }

    @Test
    void escapesUserTextInTheXhtml() {
        assertThat(pdfExporter.renderHtml(ReportFixtures.sampleReport()))
                .contains("Weekly &lt;groceries&gt;")
                .doesNotContain("Weekly <groceries>");
    }

    @Test
    void emptyMonthRendersOnOnePage() throws IOException {
        MonthlyReport full = ReportFixtures.sampleReport();
        MonthlyReport empty = new MonthlyReport(full.month(), full.monthLabel(), List.of(), full.generatedAt(),
                List.of(), List.of(), List.of(), List.of(), List.of(), false);

        Path file = pdfExporter.export(empty, outbox);

        try (PdfReader reader = new PdfReader(file.toString())) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("No transactions were recorded for October 2026.");
        }
    }

    @Test
    void replacesAnEarlierPdfAndLeavesNoTempFiles() throws IOException {
        pdfExporter.export(ReportFixtures.sampleReport(), outbox);
        pdfExporter.export(ReportFixtures.sampleReport(), outbox);

        try (Stream<Path> files = Files.list(outbox)) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("2026-10-report.pdf");
        }
    }
}
