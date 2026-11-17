package com.parcels.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

/**
 * Settings for the report files written to the outbox folder.
 *
 * @param outboxDir       folder the spreadsheets and PDFs are written to
 * @param liveSpreadsheet rewrite {@code <yyyy-MM>-report.xlsx} and {@code transactions.csv} whenever data changes (and at startup)
 * @param pdf             the monthly PDF snapshot
 */
@ConfigurationProperties("pft.reports")
public record ReportProperties(Path outboxDir,
                               @DefaultValue("true") boolean liveSpreadsheet,
                               @DefaultValue Pdf pdf) {

    /**
     * @param enabled    whether the PDF job runs (a check at startup plus a daily check)
     * @param dayOfMonth from this day on, last month's PDF is generated if it does not exist yet (1–28)
     * @param checkCron  when the daily check runs
     */
    public record Pdf(@DefaultValue("true") boolean enabled,
                      @DefaultValue("2") int dayOfMonth,
                      @DefaultValue("0 0 8 * * *") String checkCron) {

        public Pdf {
            if (dayOfMonth < 1 || dayOfMonth > 28) {
                throw new IllegalArgumentException("pft.reports.pdf.day-of-month must be between 1 and 28, was " + dayOfMonth);
            }
        }
    }
}
