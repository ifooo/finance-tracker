package com.parcels.report;

import com.parcels.domain.enums.Currency;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.transaction.TransactionService;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.parcels.domain.enums.TransactionType.EXPENSE;
import static org.assertj.core.api.Assertions.assertThat;

class LiveSpreadsheetUpdaterTest extends BaseDbTest {

    private static final Path OUTBOX = tempDir();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("pft.reports.outbox-dir", OUTBOX::toString);
        registry.add("pft.reports.live-spreadsheet", () -> "true");
    }

    @Autowired
    private TransactionService transactionService;

    private final List<Long> committed = new ArrayList<>();

    @Test
    void committedChangeRewritesThatMonthsSpreadsheet() {
        committed.add(transactionService.save(command(OffsetDateTime.of(2023, 2, 10, 12, 0, 0, 0, ZoneOffset.UTC))).id());
        assertThat(OUTBOX.resolve("2023-02-report.xlsx")).doesNotExist(); // not before the commit

        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertThat(OUTBOX.resolve("2023-02-report.xlsx")).exists();
        assertThat(OUTBOX.resolve("transactions.csv")).content().contains(",9.99,-9.99,EUR,").contains(",live,");
    }

    @Test
    void rolledBackChangeWritesNothing() {
        transactionService.save(command(OffsetDateTime.of(2023, 7, 10, 12, 0, 0, 0, ZoneOffset.UTC)));

        TestTransaction.end(); // rollback

        assertThat(OUTBOX.resolve("2023-07-report.xlsx")).doesNotExist();
    }

    /** Committed rows outlive the test, so remove them in a fresh transaction. */
    @AfterEach
    void cleanUp() {
        if (committed.isEmpty()) {
            return;
        }
        if (!TestTransaction.isActive()) {
            TestTransaction.start();
        }
        committed.forEach(transactionService::delete);
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    private static TransactionUpdateCommand command(OffsetDateTime date) {
        return new TransactionUpdateCommand(null, EXPENSE, new BigDecimal("9.99"), Currency.EUR, "live", List.of(), date, null);
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("pft-live-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
