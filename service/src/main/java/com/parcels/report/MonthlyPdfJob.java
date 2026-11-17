package com.parcels.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates last month's PDF from {@code pft.reports.pdf.day-of-month} (default the 2nd) on, which leaves a day to
 * add late receipts. Runs daily and once at startup, so a run missed while the machine was off is caught up.
 * Disable with {@code pft.reports.pdf.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "pft.reports.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyPdfJob {

    private final ReportFileService reportFileService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        check();
    }

    @Scheduled(cron = "${pft.reports.pdf.check-cron:0 0 8 * * *}")
    public void check() {
        try {
            reportFileService.generatePreviousMonthPdfIfDue().ifPresent(pdf -> log.info("Report: monthly PDF generated: {}", pdf));
        } catch (RuntimeException e) {
            log.error("Report: monthly PDF failed; it will be retried at the next check", e);
        }
    }
}
