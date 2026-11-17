package com.parcels.inbox;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.TransactionItem;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.domain.enums.TransactionType;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.ImportFileRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.repository.UserAccountRepository;
import com.parcels.domain.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class JsonTransactionFileHandlerTest extends BaseDbTest {

    @TempDir
    Path dir;

    @Autowired
    private JsonTransactionFileHandler handler;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ImportFileRepository importFileRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        List.of("Groceries", "Dining/Restaurants", "Utilities (Electricity, Water, Gas)", "Salary")
                .forEach(name -> categoryRepository.save(Category.builder().name(name).build()));
        userAccountRepository.save(UserAccount.builder().username("ana").fullName("Ana Petrova").build());
    }

    @Test
    void entriesWithoutPersonBelongToTheDefaultPerson() throws IOException {
        handler.process(file("mine.json", single("\"categories\": [\"Groceries\"]")));

        assertThat(transactionRepository.findAll()).singleElement()
                .satisfies(t -> assertThat(t.getUserAccount().getId()).isEqualTo(1L));
    }

    @Test
    void filePersonAppliesToAllEntriesUnlessAnEntrySaysOtherwise() throws IOException {
        handler.process(file("household.json", """
                { "person": "ana", "transactions": [
                    { "type": "EXPENSE", "date": "2026-11-15", "amount": 1, "currency": "MKD", "categories": ["Groceries"] },
                    { "person": "IVO", "type": "EXPENSE", "date": "2026-11-15", "amount": 2, "currency": "MKD", "categories": ["Groceries"] },
                    { "person": "Ana Petrova", "type": "EXPENSE", "date": "2026-11-15", "amount": 3, "currency": "MKD", "categories": ["Groceries"] }
                ] }
                """));

        assertThat(transactionRepository.findAll().stream()
                .sorted(Comparator.comparing(Transaction::getAmount))
                .map(t -> t.getUserAccount().getUsername()))
                .containsExactly("ana", userAccountRepository.findById(1L).orElseThrow().getUsername(), "ana");
    }

    @Test
    void unknownPersonIsRejectedWithTheKnownPeople() throws IOException {
        Path file = file("who.json", single("\"categories\": [\"Groceries\"], \"person\": \"marko\""));

        assertThatThrownBy(() -> handler.process(file))
                .isInstanceOf(InboxFileException.class)
                .hasMessageStartingWith("transactions[0].person: unknown person \"marko\". Known people: ")
                .hasMessageContaining("ana (Ana Petrova)");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void importsAllTransactionsWithItemsAndCategories() throws IOException {
        InboxResult result = handler.process(file("ok.json", """
                {
                  "$schema": "./transactions.schema.json",
                  "transactions": [
                    {
                      "externalId": "r-1", "type": "EXPENSE", "date": "2026-11-15", "amount": 1250.00, "currency": "MKD",
                      "merchant": "Vero", "description": "Weekly groceries", "categories": ["groceries", "Utilities"],
                      "paymentMethod": "CARD", "notes": "n",
                      "items": [ { "name": "Milk", "quantity": 2, "unitPrice": 65.00 }, { "name": "Bread", "total": 45 } ]
                    },
                    { "type": "income", "date": "2026-11-10T09:30:00+01:00", "amount": 100, "currency": "EUR", "categories": ["Salary"] }
                  ]
                }
                """));

        assertThat(result).isEqualTo(InboxResult.imported(2));
        List<Transaction> saved = transactionRepository.findAll().stream().sorted(Comparator.comparing(Transaction::getAmount).reversed()).toList();
        assertThat(saved).hasSize(2);

        Transaction groceries = saved.getFirst();
        assertThat(groceries.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(groceries.getAmount()).isEqualByComparingTo("1250.00");
        assertThat(groceries.getCurrency()).isEqualTo(Currency.MKD);
        assertThat(groceries.getMerchant()).isEqualTo("Vero");
        assertThat(groceries.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(groceries.getSource()).isEqualTo(TransactionSource.INBOX_JSON);
        assertThat(groceries.getExternalId()).isEqualTo("r-1");
        assertThat(groceries.getImportFile().getFileName()).isEqualTo("ok.json");
        assertThat(groceries.getCategories()).extracting(Category::getName)
                .containsExactly("Groceries", "Utilities (Electricity, Water, Gas)");
        assertThat(groceries.getItems()).extracting(TransactionItem::getName, i -> i.getTotal().toPlainString())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Milk", "130.00"), org.assertj.core.groups.Tuple.tuple("Bread", "45"));

        Transaction salary = saved.get(1);
        assertThat(salary.getTransactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(salary.getDateFrom().toInstant()).isEqualTo(OffsetDateTime.of(2026, 11, 10, 8, 30, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    void plainDateIsStartOfDayInLocalZone() {
        OffsetDateTime parsed = JsonTransactionFileHandler.parseDate("2026-03-29", ZoneId.of("Europe/Skopje"));
        assertThat(parsed.toLocalDateTime()).isEqualTo(LocalDate.of(2026, 3, 29).atStartOfDay());
        assertThat(JsonTransactionFileHandler.parseDate("2026-03-29T10:15", ZoneId.of("UTC"))).isEqualTo("2026-03-29T10:15Z");
        assertThat(JsonTransactionFileHandler.parseDate("29.03.2026", ZoneId.of("UTC"))).isNull();
    }

    @Test
    void categoryCanBeReferencedByEitherSideOfSlash() throws IOException {
        handler.process(file("a.json", single("\"categories\": [\"Restaurants\"]")));
        handler.process(file("b.json", single("\"categories\": [\"dining\"], \"amount\": 2")));

        assertThat(transactionRepository.findAll()).allSatisfy(t ->
                assertThat(t.getCategories()).extracting(Category::getName).containsExactly("Dining/Restaurants"));
    }

    @Test
    void sameFileTwiceIsImportedOnce() throws IOException {
        String content = single("\"categories\": [\"Groceries\"]");
        assertThat(handler.process(file("first.json", content))).isEqualTo(InboxResult.imported(1));
        assertThat(handler.process(file("renamed-copy.json", content))).isEqualTo(InboxResult.alreadyImported());

        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(importFileRepository.count()).isEqualTo(1);
    }

    @Test
    void validationErrorsNameEachEntryAndNothingIsStored() throws IOException {
        Path file = file("bad.json", """
                { "transactions": [
                    { "type": "EXPENSE", "date": "2026-11-15", "amount": 10, "currency": "MKD", "categories": ["Groceries"] },
                    { "type": "EXPENSE", "date": "2026-11-15", "amount": -5, "currency": "MKD", "categories": [] },
                    { "type": "EXPENSE", "date": "2026-11-15", "currency": "MKD", "categories": ["Groceries"] }
                ] }
                """);

        InboxFileException e = catchThrowableOfType(InboxFileException.class, () -> handler.process(file));

        assertThat(e.getErrors()).containsExactly(
                "transactions[1].amount: must be greater than 0",
                "transactions[1].categories: must not be empty",
                "transactions[2].amount: must not be null");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void semanticErrorsAreCollectedAcrossEntries() throws IOException {
        Path file = file("bad.json", """
                { "transactions": [
                    { "externalId": "x", "type": "EXPENSE", "date": "15.11.2026", "amount": 10, "currency": "MKD", "categories": ["Groceris"] },
                    { "externalId": "x", "type": "EXPENSE", "date": "2026-11-15", "amount": 10, "currency": "MKD", "categories": ["Groceries"],
                      "items": [ { "name": "No price" } ] }
                ] }
                """);

        InboxFileException e = catchThrowableOfType(InboxFileException.class, () -> handler.process(file));

        assertThat(e.getErrors()).hasSize(4);
        assertThat(e.getErrors().get(0)).isEqualTo("transactions[0].date: expected yyyy-MM-dd or an ISO-8601 date-time, got \"15.11.2026\"");
        assertThat(e.getErrors().get(1)).startsWith("transactions[0].categories: unknown category \"Groceris\". Known categories: Dining/Restaurants, Groceries, Salary, Utilities");
        assertThat(e.getErrors().get(2)).isEqualTo("transactions[1].externalId: \"x\" is used more than once in this file");
        assertThat(e.getErrors().get(3)).isEqualTo("transactions[1].items[0]: give either \"total\" or both \"quantity\" and \"unitPrice\"");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void externalIdAlreadyImportedIsRejected() throws IOException {
        handler.process(file("a.json", single("\"externalId\": \"receipt-7\", \"categories\": [\"Groceries\"]")));

        assertThatThrownBy(() -> handler.process(file("b.json", single("\"externalId\": \"receipt-7\", \"categories\": [\"Groceries\"], \"amount\": 3"))))
                .isInstanceOf(InboxFileException.class)
                .hasMessage("transactions[0].externalId: \"receipt-7\" was already imported");
    }

    @Test
    void misspelledPropertyIsRejectedWithPath() throws IOException {
        Path file = file("typo.json", """
                { "transactions": [ { "type": "EXPENSE", "date": "2026-11-15", "ammount": 10, "currency": "MKD", "categories": ["Groceries"] } ] }
                """);

        assertThatThrownBy(() -> handler.process(file))
                .isInstanceOf(InboxFileException.class)
                .hasMessageStartingWith("transactions[0].ammount: Unrecognized property \"ammount\"")
                .hasMessageNotContaining("com.parcels");
    }

    @Test
    void unknownEnumValueListsAllowedValues() throws IOException {
        Path file = file("enum.json", single("\"categories\": [\"Groceries\"], \"currency\": \"GBP\""));

        assertThatThrownBy(() -> handler.process(file))
                .isInstanceOf(InboxFileException.class)
                .hasMessageStartingWith("transactions[0].currency:")
                .hasMessageContaining("not one of the values accepted")
                .hasMessageContaining("EUR").hasMessageContaining("MKD").hasMessageContaining("USD");
    }

    @Test
    void malformedJsonReportsLineAndColumn() throws IOException {
        Path file = file("broken.json", "{ \"transactions\": [\n  { \"type\": \"EXPENSE\", }\n] }");

        assertThatThrownBy(() -> handler.process(file))
                .isInstanceOf(InboxFileException.class)
                .hasMessageContaining("(line 2,");
    }

    @Test
    void emptyTransactionListIsRejected() throws IOException {
        assertThatThrownBy(() -> handler.process(file("empty.json", "{ \"transactions\": [] }")))
                .isInstanceOf(InboxFileException.class)
                .hasMessage("transactions: must not be empty");
    }

    /** One otherwise valid transaction; later keys in {@code extra} override the defaults (Jackson keeps the last value). */
    private static String single(String extra) {
        return """
                { "transactions": [ { "type": "EXPENSE", "date": "2026-11-15", "amount": 1, "currency": "MKD", %s } ] }
                """.formatted(extra);
    }

    private Path file(String name, String content) throws IOException {
        return Files.writeString(dir.resolve(name), content);
    }
}
