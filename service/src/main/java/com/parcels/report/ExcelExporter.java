package com.parcels.report;

import com.parcels.report.dto.CategoryTotal;
import com.parcels.report.dto.CurrencySummary;
import com.parcels.report.dto.MonthlyReport;
import com.parcels.report.dto.PersonSummary;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionItemDto;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.parcels.domain.enums.TransactionType.EXPENSE;

/**
 * Writes a {@link MonthlyReport} to {@code <outbox>/<yyyy-MM>-report.xlsx}, replacing an earlier export of the same month.
 */
@Component
public class ExcelExporter {

    public Path export(MonthlyReport report, Path outboxDir) {
        Objects.requireNonNull(outboxDir, "pft.reports.outbox-dir is not configured");
        Path target = outboxDir.resolve(report.month() + "-report.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            ZoneId zone = report.generatedAt().getOffset();
            summarySheet(workbook.createSheet("Summary"), report, styles);
            transactionsSheet(workbook.createSheet("Transactions"), report.transactions(), zone, styles);
            categorySheet(workbook.createSheet("By category"), report.categories(), report.people(), styles);
            itemsSheet(workbook.createSheet("Items"), report.transactions(), zone, styles);

            Files.createDirectories(outboxDir);
            // Write next to the target first so a half-written file never replaces a good one.
            Path temp = Files.createTempFile(outboxDir, ".report-", ".xlsx.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                workbook.write(out);
            }
            return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }

    private static void summarySheet(Sheet sheet, MonthlyReport report, Styles styles) {
        Row title = sheet.createRow(0);
        text(title, 0, "Finance report " + report.monthLabel(), styles.title);
        text(sheet.createRow(1), 0, "Generated " + report.generatedAt().toLocalDateTime().withNano(0).toString().replace('T', ' '), null);
        text(sheet.createRow(2), 0, "Amounts in different currencies are never added together.", null);

        int r = header(sheet, 4, styles, "Currency", "Income", "Expense", "Net", "Prev. income", "Prev. expense", "Expense change %");
        for (CurrencySummary s : report.totals()) {
            Row row = sheet.createRow(r++);
            text(row, 0, s.currency().name(), null);
            money(row, 1, s.income(), styles);
            money(row, 2, s.expense(), styles);
            money(row, 3, s.net(), styles);
            money(row, 4, s.previousIncome(), styles);
            money(row, 5, s.previousExpense(), styles);
            number(row, 6, s.expenseChangePercent(), styles.percent);
        }

        r = header(sheet, r + 1, styles, "Person", "Currency", "Income", "Expense", "Net");
        for (PersonSummary p : report.personTotals()) {
            Row row = sheet.createRow(r++);
            text(row, 0, p.person(), null);
            text(row, 1, p.currency().name(), null);
            money(row, 2, p.income(), styles);
            money(row, 3, p.expense(), styles);
            money(row, 4, p.net(), styles);
        }
        widths(sheet, 14, 14, 14, 14, 14, 14, 18);
        sheet.setColumnWidth(0, 22 * 256);
    }

    private static void transactionsSheet(Sheet sheet, List<TransactionDto> transactions, ZoneId zone, Styles styles) {
        String[] headers = {"Date", "Person", "Type", "Amount", "Signed amount", "Currency", "Categories", "Merchant", "Description", "Payment", "Source", "Notes"};
        int r = header(sheet, 0, styles, headers);
        for (TransactionDto t : transactions) {
            Row row = sheet.createRow(r++);
            date(row, 0, t.dateFrom() == null ? null : t.dateFrom().atZoneSameInstant(zone).toLocalDate(), styles);
            text(row, 1, t.person(), null);
            text(row, 2, name(t.transactionType()), null);
            money(row, 3, t.amount(), styles);
            money(row, 4, t.amount() == null ? null : t.transactionType() == EXPENSE ? t.amount().negate() : t.amount(), styles);
            text(row, 5, name(t.currency()), null);
            text(row, 6, String.join(", ", t.categoryNames()), null);
            text(row, 7, t.merchant(), null);
            text(row, 8, t.description(), null);
            text(row, 9, name(t.paymentMethod()), null);
            text(row, 10, name(t.source()), null);
            text(row, 11, t.notes(), null);
        }
        filterAndFreeze(sheet, r, headers.length);
        widths(sheet, 12, 18, 10, 13, 14, 10, 28, 20, 32, 14, 12, 30);
    }

    /** One column per person after the total, so the household split is visible per category. */
    private static void categorySheet(Sheet sheet, List<CategoryTotal> categories, List<String> people, Styles styles) {
        List<String> headers = new ArrayList<>(List.of("Currency", "Category", "Expense", "Transactions", "Share %"));
        headers.addAll(people);
        int r = header(sheet, 0, styles, headers.toArray(String[]::new));
        for (CategoryTotal c : categories) {
            Row row = sheet.createRow(r++);
            text(row, 0, c.currency().name(), null);
            text(row, 1, c.category(), null);
            money(row, 2, c.amount(), styles);
            row.createCell(3).setCellValue(c.count());
            number(row, 4, c.sharePercent(), styles.percent);
            for (int i = 0; i < people.size(); i++) {
                money(row, 5 + i, c.byPerson().get(people.get(i)), styles);
            }
        }
        filterAndFreeze(sheet, r, headers.size());
        widths(sheet, 10, 34, 14, 13, 10);
        for (int i = 0; i < people.size(); i++) {
            sheet.setColumnWidth(5 + i, 16 * 256);
        }
    }

    private static void itemsSheet(Sheet sheet, List<TransactionDto> transactions, ZoneId zone, Styles styles) {
        String[] headers = {"Date", "Merchant", "Item", "Quantity", "Unit price", "Total", "Currency"};
        int r = header(sheet, 0, styles, headers);
        for (TransactionDto t : transactions) {
            for (TransactionItemDto item : t.items()) {
                Row row = sheet.createRow(r++);
                date(row, 0, t.dateFrom() == null ? null : t.dateFrom().atZoneSameInstant(zone).toLocalDate(), styles);
                text(row, 1, t.merchant(), null);
                text(row, 2, item.name(), null);
                number(row, 3, item.quantity(), null);
                money(row, 4, item.unitPrice(), styles);
                money(row, 5, item.total(), styles);
                text(row, 6, name(t.currency()), null);
            }
        }
        filterAndFreeze(sheet, r, headers.length);
        widths(sheet, 12, 20, 32, 10, 12, 12, 10);
    }

    private static int header(Sheet sheet, int rowIndex, Styles styles, String... headers) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            text(row, i, headers[i], styles.header);
        }
        return rowIndex + 1;
    }

    private static void filterAndFreeze(Sheet sheet, int lastRowExclusive, int columns) {
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, lastRowExclusive - 1), 0, columns - 1));
        sheet.createFreezePane(0, 1);
    }

    private static void widths(Sheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void money(Row row, int column, BigDecimal value, Styles styles) {
        number(row, column, value, styles.money);
    }

    private static void number(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void date(Row row, int column, LocalDate value, Styles styles) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(styles.date);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle title;
        final CellStyle money;
        final CellStyle percent;
        final CellStyle date;

        Styles(XSSFWorkbook workbook) {
            Font bold = workbook.createFont();
            bold.setBold(true);

            header = workbook.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            Font big = workbook.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(big);

            money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

            date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        }
    }
}
