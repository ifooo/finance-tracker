package com.parcels.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

/**
 * Keeps the monthly spreadsheets and {@code transactions.csv} current: rewrites them right after a change is committed,
 * and at startup (the current and previous month, plus the CSV), which covers changes made while the app was not running.
 * Disable with {@code pft.reports.live-spreadsheet=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "pft.reports", name = "live-spreadsheet", havingValue = "true", matchIfMissing = true)
public class LiveSpreadsheetUpdater {

    private final ReportFileService reportFileService;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChange(ReportDataChangedEvent event) {
        refresh(event.months());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        YearMonth now = YearMonth.now(clock);
        refresh(List.of(now.minusMonths(1), now));
    }

    private void refresh(Collection<YearMonth> months) {
        try {
            reportFileService.refreshSpreadsheets(months);
        } catch (RuntimeException e) {
            // The data is already stored; a failed export must not break the import or API call.
            log.error("Report: could not update spreadsheet(s) for {}", months, e);
        }
        try {
            reportFileService.refreshTransactionsCsv();
        } catch (RuntimeException e) {
            log.error("Report: could not update the transactions CSV", e);
        }
    }
}
