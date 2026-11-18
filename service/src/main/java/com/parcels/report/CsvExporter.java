package com.parcels.report;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.parcels.domain.enums.TransactionType.EXPENSE;

/**
 * Writes every transaction to {@code <outbox>/transactions.csv} (RFC 4180, comma-separated, dot decimals).
 * UTF-8 with a byte-order mark, so Excel opens Cyrillic text correctly.
 */
@Component
public class CsvExporter {

    static final String FILE_NAME = "transactions.csv";
    static final List<String> HEADER = List.of("id", "date", "datetime", "person", "type", "amount", "signed_amount", "currency",
            "categories", "merchant", "description", "payment_method", "source", "notes");

    public Path export(List<Transaction> transactions, ZoneId zone, Path outboxDir) {
        Objects.requireNonNull(outboxDir, "pft.reports.outbox-dir is not configured");
        Path target = outboxDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(outboxDir);
            Path temp = Files.createTempFile(outboxDir, ".transactions-", ".csv.tmp");
            try (BufferedWriter out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                out.write('﻿');
                writeRow(out, HEADER);
                for (Transaction t : transactions) {
                    writeRow(out, row(t, zone));
                }
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(temp);
                throw e;
            }
            return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }

    private static List<String> row(Transaction t, ZoneId zone) {
        var local = t.getDateFrom() == null ? null : t.getDateFrom().atZoneSameInstant(zone);
        BigDecimal signed = t.getAmount() == null ? null : t.getTransactionType() == EXPENSE ? t.getAmount().negate() : t.getAmount();
        return Arrays.asList(
                str(t.getId()),
                local == null ? "" : local.toLocalDate().toString(),
                local == null ? "" : local.toOffsetDateTime().toString(),
                t.getUserAccount() == null ? "" : t.getUserAccount().displayName(),
                str(t.getTransactionType()),
                t.getAmount() == null ? "" : t.getAmount().toPlainString(),
                signed == null ? "" : signed.toPlainString(),
                str(t.getCurrency()),
                t.getCategories().stream().map(Category::getName).collect(Collectors.joining("; ")),
                str(t.getMerchant()),
                str(t.getDescription()),
                str(t.getPaymentMethod()),
                str(t.getSource()),
                str(t.getNotes()));
    }

    private static void writeRow(BufferedWriter out, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(escape(values.get(i)));
        }
        out.write("\r\n");
    }

    /** Quotes a value containing a comma, quote or line break, doubling inner quotes (RFC 4180). */
    static String escape(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
