package com.parcels.report;

import com.parcels.config.ReportProperties;
import com.parcels.domain.ReportRun;
import com.parcels.report.dto.ReportFiles;
import com.parcels.repository.ReportRunRepository;
import com.parcels.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Writes the report files in the outbox: one spreadsheet per month and a CSV of all transactions (both kept current),
 * and a PDF snapshot per month.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportFileService {

    private final ReportService reportService;
    private final ExcelExporter excelExporter;
    private final PdfExporter pdfExporter;
    private final CsvExporter csvExporter;
    private final TransactionRepository transactionRepository;
    private final ReportRunRepository reportRunRepository;
    private final ReportProperties properties;
    private final Clock clock;

    /** Rewrites {@code <yyyy-MM>-report.xlsx} for each month. Synchronized so two refreshes never interleave. */
    public synchronized void refreshSpreadsheets(Collection<YearMonth> months) {
        for (YearMonth month : new TreeSet<>(months)) {
            Path file = excelExporter.export(reportService.monthly(month), properties.outboxDir());
            log.info("Report: spreadsheet for {} updated: {}", month, file);
        }
    }

    /** Rewrites {@code transactions.csv} with every transaction. */
    public synchronized Path refreshTransactionsCsv() {
        Path file = csvExporter.export(transactionRepository.findAllForExport(), clock.getZone(), properties.outboxDir());
        log.info("Report: {} updated", file);
        return file;
    }

    /** Generates last month's PDF if today is on or after the configured day and it was not generated yet. */
    public Optional<Path> generatePreviousMonthPdfIfDue() {
        return generatePreviousMonthPdfIfDue(LocalDate.now(clock));
    }

    // package-private so tests can pick the date
    Optional<Path> generatePreviousMonthPdfIfDue(LocalDate today) {
        YearMonth previous = YearMonth.from(today).minusMonths(1);
        if (today.getDayOfMonth() < properties.pdf().dayOfMonth() || reportRunRepository.existsByMonth(previous.toString())) {
            return Optional.empty();
        }
        ReportFiles files = writeAll(previous);
        reportRunRepository.save(ReportRun.builder().month(previous.toString()).generatedAt(OffsetDateTime.now(clock)).build());
        return Optional.of(files.pdf());
    }

    /** Writes both files for a month now (on demand). Does not affect the scheduled PDF. */
    public synchronized ReportFiles writeAll(YearMonth month) {
        var report = reportService.monthly(month);
        Path spreadsheet = excelExporter.export(report, properties.outboxDir());
        Path pdf = pdfExporter.export(report, properties.outboxDir());
        log.info("Report: wrote {} and {}", spreadsheet, pdf);
        return new ReportFiles(month, spreadsheet, pdf);
    }
}
