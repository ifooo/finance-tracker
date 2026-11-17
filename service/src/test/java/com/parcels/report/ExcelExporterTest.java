package com.parcels.report;

import com.parcels.report.dto.MonthlyReport;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExporterTest {

    @TempDir
    Path outbox;

    private final ExcelExporter exporter = new ExcelExporter();

    @Test
    void writesOneSheetPerSectionWithAPerPersonSplit() throws IOException {
        MonthlyReport report = ReportFixtures.sampleReport();

        Path file = exporter.export(report, outbox.resolve("nested"));

        assertThat(file.getFileName()).hasToString("2026-10-report.xlsx");
        try (InputStream in = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(in)) {
            assertThat(IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName))
                    .containsExactly("Summary", "Transactions", "By category", "Items");

            Sheet summary = workbook.getSheet("Summary");
            assertThat(summary.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Finance report October 2026");
            assertThat(summary.getRow(6).getCell(0).getStringCellValue()).isEqualTo("MKD");
            assertThat(summary.getRow(6).getCell(3).getNumericCellValue()).isEqualTo(58350.00);
            assertThat(summary.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Person");
            assertThat(text(summary.getRow(11), 0) + " " + text(summary.getRow(11), 1)).isEqualTo("Ana Test MKD");
            assertThat(summary.getRow(11).getCell(3).getNumericCellValue()).isEqualTo(400.00);

            Sheet transactions = workbook.getSheet("Transactions");
            assertThat(transactions.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Person");
            assertThat(transactions.getLastRowNum()).isEqualTo(4);
            assertThat(transactions.getRow(2).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(transactions.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Ivo Test");
            assertThat(transactions.getRow(2).getCell(4).getNumericCellValue()).isEqualTo(-1250.00);
            assertThat(transactions.getRow(3).getCell(6).getStringCellValue()).isEqualTo("Dining/Restaurants, Entertainment");

            Sheet categories = workbook.getSheet("By category");
            assertThat(text(categories.getRow(0), 5) + "|" + text(categories.getRow(0), 6)).isEqualTo("Ivo Test|Ana Test");
            Row groceries = categories.getRow(3);
            assertThat(groceries.getCell(1).getStringCellValue()).isEqualTo("Groceries");
            assertThat(groceries.getCell(5).getNumericCellValue()).isEqualTo(1250.00);
            assertThat(groceries.getCell(6).getNumericCellValue()).isEqualTo(400.00);

            assertThat(workbook.getSheet("Items").getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void exportingTheSameMonthAgainReplacesTheFileAndLeavesNoTempFiles() throws IOException {
        MonthlyReport report = ReportFixtures.sampleReport();

        exporter.export(report, outbox);
        exporter.export(report, outbox);

        try (Stream<Path> files = Files.list(outbox)) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("2026-10-report.xlsx");
        }
    }

    private static String text(Row row, int column) {
        return row.getCell(column).getStringCellValue();
    }
}
