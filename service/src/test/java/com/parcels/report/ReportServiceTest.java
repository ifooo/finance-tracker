package com.parcels.report;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.UserAccount;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.report.dto.CategoryTotal;
import com.parcels.report.dto.CurrencySummary;
import com.parcels.report.dto.MerchantTotal;
import com.parcels.report.dto.MonthlyReport;
import com.parcels.report.dto.PersonSummary;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.parcels.domain.enums.Currency.EUR;
import static com.parcels.domain.enums.Currency.MKD;
import static com.parcels.domain.enums.TransactionType.EXPENSE;
import static com.parcels.domain.enums.TransactionType.INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class ReportServiceTest extends BaseDbTest {

    private static final YearMonth MARCH = YearMonth.of(2025, 3);

    @Autowired
    private ReportService reportService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private Category groceries;
    private Category dining;
    private Category salary;
    private UserAccount me;
    private UserAccount partner;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        groceries = categoryRepository.save(Category.builder().name("Groceries").build());
        dining = categoryRepository.save(Category.builder().name("Dining").build());
        salary = categoryRepository.save(Category.builder().name("Salary").build());
        me = userAccountRepository.findById(1L).orElseThrow();
        partner = userAccountRepository.save(UserAccount.builder().username("ana").fullName("Ana Test").build());
    }

    @Test
    void totalsArePerCurrencyAndComparedWithPreviousMonth() {
        save(me, INCOME, MKD, "60000", at(3, 1), null, salary);
        save(me, EXPENSE, MKD, "1000", at(3, 5), "Vero", groceries);
        save(me, EXPENSE, MKD, "500", at(3, 31), "Vero", groceries);
        save(me, EXPENSE, EUR, "20", at(3, 10), "Trattoria", dining);
        save(me, EXPENSE, MKD, "1000", at(2, 20), "Vero", groceries);   // previous month
        save(me, EXPENSE, MKD, "9999", at(4, 1), "Vero", groceries);    // next month: ignored

        MonthlyReport report = reportService.monthly(MARCH);

        assertThat(report.monthLabel()).isEqualTo("March 2025");
        assertThat(report.people()).containsExactly(me.displayName());
        assertThat(report.household()).isFalse();
        assertThat(report.transactions()).hasSize(4);
        assertThat(report.totals()).extracting(CurrencySummary::currency).containsExactly(EUR, MKD);

        CurrencySummary mkd = report.totals().get(1);
        assertThat(mkd.income()).isEqualByComparingTo("60000");
        assertThat(mkd.expense()).isEqualByComparingTo("1500");
        assertThat(mkd.net()).isEqualByComparingTo("58500");
        assertThat(mkd.previousExpense()).isEqualByComparingTo("1000");
        assertThat(mkd.expenseChangePercent()).isEqualByComparingTo("50.0");

        CurrencySummary eur = report.totals().getFirst();
        assertThat(eur.expense()).isEqualByComparingTo("20");
        assertThat(eur.expenseChangePercent()).isNull();
    }

    @Test
    void householdTotalsAreSplitPerPersonAndCurrency() {
        save(me, INCOME, MKD, "60000", at(3, 1), null, salary);
        save(me, EXPENSE, MKD, "1000", at(3, 5), null, groceries);
        save(partner, EXPENSE, MKD, "400", at(3, 6), null, groceries);
        save(partner, EXPENSE, EUR, "15", at(3, 7), null, dining);

        MonthlyReport report = reportService.monthly(MARCH);

        assertThat(report.people()).containsExactly(me.displayName(), "Ana Test");
        assertThat(report.household()).isTrue();
        assertThat(report.totals()).filteredOn(t -> t.currency() == MKD).singleElement()
                .satisfies(t -> assertThat(t.expense()).isEqualByComparingTo("1400"));
        assertThat(report.personTotals())
                .extracting(PersonSummary::person, PersonSummary::currency, p -> p.expense().toPlainString(), p -> p.net().toPlainString())
                .containsExactly(
                        tuple(me.displayName(), MKD, "1000", "59000"),
                        tuple("Ana Test", EUR, "15", "-15"),
                        tuple("Ana Test", MKD, "400", "-400"));
        assertThat(report.transactions()).extracting(t -> t.personUsername()).contains(me.getUsername(), "ana");

        CategoryTotal groceriesTotal = report.categories().stream().filter(c -> c.category().equals("Groceries")).findFirst().orElseThrow();
        assertThat(groceriesTotal.amount()).isEqualByComparingTo("1400");
        assertThat(groceriesTotal.byPerson()).containsExactly(
                Map.entry(me.displayName(), new BigDecimal("1000")), Map.entry("Ana Test", new BigDecimal("400")));
        CategoryTotal diningTotal = report.categories().stream().filter(c -> c.category().equals("Dining")).findFirst().orElseThrow();
        assertThat(diningTotal.byPerson()).containsOnlyKeys("Ana Test");
    }

    @Test
    void categoryTotalsAreExpensesOnlyAndCountMultiCategoryTransactionsUnderEach() {
        save(me, EXPENSE, MKD, "300", at(3, 2), null, groceries, dining);
        save(me, EXPENSE, MKD, "100", at(3, 3), null, groceries);
        save(me, EXPENSE, MKD, "50", at(3, 4), null);
        save(me, INCOME, MKD, "5000", at(3, 1), null, salary);

        MonthlyReport report = reportService.monthly(MARCH);

        assertThat(report.multiCategoryTransactions()).isTrue();
        assertThat(report.categories())
                .extracting(CategoryTotal::category, c -> c.amount().toPlainString(), CategoryTotal::count, c -> c.sharePercent().toPlainString())
                .containsExactly(
                        tuple("Groceries", "400", 2, "88.9"),
                        tuple("Dining", "300", 1, "66.7"),
                        tuple(ReportService.UNCATEGORIZED, "50", 1, "11.1"));
        assertThat(report.totals().getFirst().expense()).isEqualByComparingTo("450");
    }

    @Test
    void topMerchantsAreRankedPerCurrency() {
        for (int i = 1; i <= 7; i++) {
            save(me, EXPENSE, MKD, String.valueOf(i * 10), at(3, i), "Shop " + i, groceries);
        }
        save(partner, EXPENSE, MKD, "5", at(3, 20), "Shop 1", groceries);
        save(me, EXPENSE, EUR, "3", at(3, 20), "Cafe", dining);

        List<MerchantTotal> merchants = reportService.monthly(MARCH).topMerchants();

        assertThat(merchants).filteredOn(m -> m.currency() == MKD)
                .extracting(MerchantTotal::merchant).containsExactly("Shop 7", "Shop 6", "Shop 5", "Shop 4", "Shop 3");
        assertThat(merchants).filteredOn(m -> m.currency() == EUR).extracting(MerchantTotal::merchant).containsExactly("Cafe");
    }

    @Test
    void emptyMonthHasNoTotals() {
        MonthlyReport report = reportService.monthly(MARCH);

        assertThat(report.totals()).isEmpty();
        assertThat(report.people()).isEmpty();
        assertThat(report.transactions()).isEmpty();
        assertThat(report.multiCategoryTransactions()).isFalse();
    }

    private static OffsetDateTime at(int month, int day) {
        return LocalDate.of(2025, month, day).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void save(UserAccount person, TransactionType type, Currency currency, String amount, OffsetDateTime date,
                      String merchant, Category... categories) {
        transactionRepository.save(Transaction.builder()
                .transactionType(type)
                .currency(currency)
                .amount(new BigDecimal(amount))
                .dateFrom(date)
                .merchant(merchant)
                .categories(new ArrayList<>(List.of(categories)))
                .userAccount(person)
                .build());
    }
}
