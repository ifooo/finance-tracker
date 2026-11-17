package com.parcels.transaction;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.repository.UserAccountRepository;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.parcels.domain.enums.TransactionType.EXPENSE;
import static com.parcels.domain.enums.TransactionType.INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceImplTest extends BaseDbTest {

    private static final OffsetDateTime JAN_15 = OffsetDateTime.of(2024, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime FEB_15 = JAN_15.plusMonths(1);
    private static final OffsetDateTime MAR_15 = JAN_15.plusMonths(2);

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        save(EXPENSE, JAN_15, "10.00");
        save(EXPENSE, FEB_15, "20.00");
        save(EXPENSE, MAR_15, "30.00");
        save(INCOME, FEB_15, "1000.00");
        entityManager.flush();
    }

    // --- Bug 1: date range filter ---

    @Test
    void filtersByTypeOnlyWhenNoDatesGiven() {
        assertThat(descriptionsOf(EXPENSE, null, null)).containsExactlyInAnyOrder("10.00", "20.00", "30.00");
    }

    @Test
    void filtersByTypeAndBothDates() {
        assertThat(descriptionsOf(EXPENSE, FEB_15.minusDays(1), FEB_15.plusDays(1))).containsExactly("20.00");
    }

    @Test
    void dateBoundsAreInclusive() {
        assertThat(descriptionsOf(EXPENSE, JAN_15, FEB_15)).containsExactlyInAnyOrder("10.00", "20.00");
    }

    @Test
    void filtersWithOnlyFrom() {
        assertThat(descriptionsOf(EXPENSE, FEB_15, null)).containsExactlyInAnyOrder("20.00", "30.00");
    }

    @Test
    void filtersWithOnlyTo() {
        assertThat(descriptionsOf(EXPENSE, null, FEB_15)).containsExactlyInAnyOrder("10.00", "20.00");
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> transactionService.getTransactionsByTypeAndDateRange(EXPENSE, MAR_15, JAN_15, 0, 10))
                .isInstanceOf(ValidationException.class);
    }

    // --- Bug 2: updated_at is persisted on update ---

    @Test
    void updatedAtChangesOnUpdateAndCreatedAtDoesNot() throws InterruptedException {
        Long id = save(EXPENSE, JAN_15, "5.00").getId();
        entityManager.flush();
        entityManager.clear();
        Transaction before = transactionRepository.findById(id).orElseThrow();
        OffsetDateTime createdAt = before.getCreatedAt();
        OffsetDateTime updatedAtBefore = before.getUpdatedAt();

        Thread.sleep(10);
        before.setDescription("changed");
        entityManager.flush();
        entityManager.clear();

        Transaction after = transactionRepository.findById(id).orElseThrow();
        assertThat(after.getUpdatedAt()).isAfter(updatedAtBefore);
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
    }

    // --- Bug 3: missing ids throw EntityNotFoundException (mapped to 404) ---

    @Test
    void getByIdThrowsNotFoundForMissingId() {
        assertThatThrownBy(() -> transactionService.getById(999_999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Transaction 999999 not found");
    }

    @Test
    void deleteThrowsNotFoundForMissingId() {
        assertThatThrownBy(() -> transactionService.delete(999_999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- Bug 4: amounts are exact decimals ---

    @Test
    void amountsAreStoredAndSummedExactly() {
        Long a = save(INCOME, MAR_15, "0.10").getId();
        Long b = save(INCOME, MAR_15, "0.20").getId();
        entityManager.flush();
        entityManager.clear();

        BigDecimal sum = transactionService.getById(a).amount().add(transactionService.getById(b).amount());

        assertThat(sum).isEqualByComparingTo("0.30");
    }

    // --- Pagination order: newest first, stable ---

    @Test
    void transactionsAreReturnedNewestFirst() {
        assertThat(descriptionsOf(EXPENSE, null, null)).containsExactly("30.00", "20.00", "10.00");
    }

    @Test
    void transactionsWithSameDateAreOrderedById() {
        Long older = save(EXPENSE, MAR_15, "first").getId();
        Long newer = save(EXPENSE, MAR_15, "second").getId();
        entityManager.flush();

        assertThat(newer).isGreaterThan(older);
        assertThat(descriptionsOf(EXPENSE, MAR_15, MAR_15)).containsExactly("second", "first", "30.00");
    }

    // --- Unknown category ids are rejected, not silently dropped ---

    @Test
    void saveRejectsUnknownCategoryIds() {
        Long existing = categoryRepository.save(Category.builder().name("Food").build()).getId();

        assertThatThrownBy(() -> transactionService.save(command(List.of(existing, 999_998L, 999_999L))))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Categories [999998, 999999] not found");
    }

    @Test
    void saveLinksCategoriesAndExposesTheirNames() {
        Long food = categoryRepository.save(Category.builder().name("Food").build()).getId();
        Long fun = categoryRepository.save(Category.builder().name("Fun").build()).getId();

        TransactionDto saved = transactionService.save(command(List.of(food, fun, food)));

        assertThat(saved.categoryIds()).containsExactlyInAnyOrder(food, fun);
        assertThat(saved.categoryNames()).containsExactlyInAnyOrder("Food", "Fun");
    }

    // --- Person ---

    @Test
    void saveAssignsTheNamedPersonOrTheDefault() {
        userAccountRepository.save(com.parcels.domain.UserAccount.builder().username("ana").fullName("Ana Petrova").build());
        TransactionUpdateCommand forAna = new TransactionUpdateCommand(null, EXPENSE, BigDecimal.TEN, Currency.MKD, "x", List.of(), JAN_15, "Ana");

        assertThat(transactionService.save(forAna).personUsername()).isEqualTo("ana");
        assertThat(transactionService.save(command(List.of())).personUsername())
                .isEqualTo(userAccountRepository.findById(1L).orElseThrow().getUsername());
    }

    @Test
    void saveRejectsUnknownPerson() {
        TransactionUpdateCommand forNobody = new TransactionUpdateCommand(null, EXPENSE, BigDecimal.TEN, Currency.MKD, "x", List.of(), JAN_15, "nobody");

        assertThatThrownBy(() -> transactionService.save(forNobody))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("unknown person \"nobody\"");
    }

    private static TransactionUpdateCommand command(List<Long> categoryIds) {
        return new TransactionUpdateCommand(null, EXPENSE, new BigDecimal("12.50"), Currency.MKD,
                "with categories", categoryIds, JAN_15, null);
    }

    private Transaction save(TransactionType type, OffsetDateTime date, String amount) {
        Transaction t = new Transaction();
        t.setTransactionType(type);
        t.setDateFrom(date);
        t.setAmount(amount.matches("[0-9.]+") ? new BigDecimal(amount) : BigDecimal.ONE);
        t.setDescription(amount); // used as a readable label in assertions
        t.setCurrency(Currency.MKD);
        t.setUserAccount(userAccountRepository.findById(1L).orElseThrow());
        return transactionRepository.save(t);
    }

    private List<String> descriptionsOf(TransactionType type, OffsetDateTime from, OffsetDateTime to) {
        return transactionService.getTransactionsByTypeAndDateRange(type, from, to, 0, 50)
                .map(TransactionDto::description)
                .getContent();
    }
}
