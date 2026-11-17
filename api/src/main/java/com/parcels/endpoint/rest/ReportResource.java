package com.parcels.endpoint.rest;

import com.parcels.report.ReportFileService;
import com.parcels.report.ReportService;
import com.parcels.report.dto.MonthlyReport;
import com.parcels.report.dto.ReportFiles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Monthly report: JSON summary, spreadsheet and PDF")
public class ReportResource {

    private final ReportService reportService;
    private final ReportFileService reportFileService;

    @GetMapping("/monthly")
    @Operation(summary = "Monthly summary as JSON", description = "Per-currency totals, categories, merchants, budgets, goals and transactions. No side effects.")
    public MonthlyReport monthly(
            @Parameter(description = "Month as yyyy-MM", example = "2026-10") @RequestParam("month") YearMonth month) {
        return reportService.monthly(month);
    }

    @PostMapping("/monthly/files")
    @Operation(summary = "Write the spreadsheet and PDF for a month now",
            description = "Writes <outbox>/<yyyy-MM>-report.xlsx and .pdf, replacing earlier versions.")
    public ReportFiles writeFiles(
            @Parameter(description = "Month as yyyy-MM", example = "2026-10") @RequestParam("month") YearMonth month) {
        return reportFileService.writeAll(month);
    }
}
