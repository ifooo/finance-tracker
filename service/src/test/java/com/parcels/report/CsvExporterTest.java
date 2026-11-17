package com.parcels.report;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.UserAccount;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @TempDir
    Path outbox;

    private final CsvExporter exporter = new CsvExporter();

    @Test
    void writesHeaderAndOneRowPerTransaction() throws IOException {
        Transaction groceries = Transaction.builder()
                .id(7L)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("1250.00"))
                .currency(Currency.MKD)
                .dateFrom(OffsetDateTime.of(2026, 10, 31, 23, 30, 0, 0, ZoneOffset.UTC)) // Nov 1 in Skopje
                .merchant("Веро Центар")
                .description("Weekly \"big\" shop, with extras")
                .paymentMethod(PaymentMethod.CARD)
                .source(TransactionSource.INBOX_JSON)
                .externalId("r-1")
                .notes("line one\nline two")
                .userAccount(UserAccount.builder().username("ana").fullName("Ana Test").build())
                .categories(new ArrayList<>(List.of(Category.builder().name("Groceries").build(),
                        Category.builder().name("Utilities (Electricity, Water, Gas)").build())))
                .build();
        Transaction salary = Transaction.builder()
                .id(8L)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("60000"))
                .currency(Currency.MKD)
                .dateFrom(OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC))
                .build();

        Path file = exporter.export(List.of(groceries, salary), ZoneId.of("Europe/Skopje"), outbox);

        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes).startsWith(0xEF, 0xBB, 0xBF); // UTF-8 BOM for Excel
        String csv = new String(bytes, StandardCharsets.UTF_8).substring(1);
        assertThat(csv).isEqualTo(
                "id,date,datetime,person,type,amount,signed_amount,currency,categories,merchant,description,payment_method,source,external_id,notes\r\n"
                + "7,2026-11-01,2026-11-01T00:30+01:00,Ana Test,EXPENSE,1250.00,-1250.00,MKD,\"Groceries; Utilities (Electricity, Water, Gas)\","
                + "Веро Центар,\"Weekly \"\"big\"\" shop, with extras\",CARD,INBOX_JSON,r-1,\"line one\nline two\"\r\n"
                + "8,2026-11-10,2026-11-10T10:00+01:00,,INCOME,60000,60000,MKD,,,,,API,,\r\n");
    }

    @Test
    void emptyDatabaseGivesHeaderOnlyAndNoTempFilesRemain() throws IOException {
        exporter.export(List.of(), ZoneId.of("UTC"), outbox);
        exporter.export(List.of(), ZoneId.of("UTC"), outbox);

        assertThat(Files.readString(outbox.resolve("transactions.csv"))).isEqualTo("﻿" + String.join(",", CsvExporter.HEADER) + "\r\n");
        try (Stream<Path> files = Files.list(outbox)) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("transactions.csv");
        }
    }

    @Test
    void escapesOnlyWhenNeeded() {
        assertThat(CsvExporter.escape("plain")).isEqualTo("plain");
        assertThat(CsvExporter.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvExporter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvExporter.escape("x\r\ny")).isEqualTo("\"x\r\ny\"");
    }
}
