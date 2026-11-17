package com.parcels.report;

import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.domain.Transaction;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import com.parcels.repository.ReportRunRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFileServiceTest extends BaseDbTest {

    private static final Path OUTBOX = tempDir();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("pft.reports.outbox-dir", OUTBOX::toString);
    }

    @Autowired
    private ReportFileService reportFileService;
    @Autowired
    private ReportRunRepository reportRunRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        reportRunRepository.deleteAll();
    }

    @Test
    void pdfIsNotDueBeforeTheConfiguredDay() {
        assertThat(reportFileService.generatePreviousMonthPdfIfDue(LocalDate.of(2026, 11, 1))).isEmpty();
        assertThat(reportRunRepository.existsByMonth("2026-10")).isFalse();
    }

    @Test
    void pdfIsGeneratedOnceFromTheConfiguredDay() {
        Optional<Path> pdf = reportFileService.generatePreviousMonthPdfIfDue(LocalDate.of(2026, 11, 2));

        assertThat(pdf).hasValueSatisfying(file -> {
            assertThat(file).exists().hasFileName("2026-10-report.pdf");
            assertThat(file.resolveSibling("2026-10-report.xlsx")).exists();
        });
        assertThat(reportRunRepository.existsByMonth("2026-10")).isTrue();
        assertThat(reportFileService.generatePreviousMonthPdfIfDue(LocalDate.of(2026, 11, 3))).isEmpty();
    }

    @Test
    void missedRunIsCaughtUpLaterInTheMonth() {
        assertThat(reportFileService.generatePreviousMonthPdfIfDue(LocalDate.of(2026, 1, 20)))
                .hasValueSatisfying(file -> assertThat(file).hasFileName("2025-12-report.pdf"));
    }

    @Test
    void refreshWritesOneSpreadsheetPerMonth() {
        reportFileService.refreshSpreadsheets(List.of(YearMonth.of(2024, 5), YearMonth.of(2024, 6), YearMonth.of(2024, 5)));

        assertThat(OUTBOX.resolve("2024-05-report.xlsx")).exists();
        assertThat(OUTBOX.resolve("2024-06-report.xlsx")).exists();
    }

    @Test
    void csvContainsEveryTransactionOldestFirst() throws IOException {
        transactionRepository.deleteAll();
        save("30.00", OffsetDateTime.of(2024, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC));
        save("10.00", OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));

        Path csv = reportFileService.refreshTransactionsCsv();

        List<String> lines = Files.readAllLines(csv);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).contains(",10.00,-10.00,EUR,");
        assertThat(lines.get(2)).contains(",30.00,-30.00,EUR,");
    }

    private void save(String amount, OffsetDateTime date) {
        transactionRepository.save(Transaction.builder().transactionType(TransactionType.EXPENSE).amount(new BigDecimal(amount))
                .currency(Currency.EUR).dateFrom(date).userAccount(userAccountRepository.findById(1L).orElseThrow()).build());
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("pft-outbox-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
