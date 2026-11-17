package com.parcels.report;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.UserAccount;
import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import com.parcels.person.PersonService;
import com.parcels.report.dto.CategoryTotal;
import com.parcels.report.dto.CurrencySummary;
import com.parcels.report.dto.MerchantTotal;
import com.parcels.report.dto.MonthlyReport;
import com.parcels.report.dto.PersonSummary;
import com.parcels.repository.TransactionRepository;
import com.parcels.transaction.dto.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.parcels.domain.enums.TransactionType.EXPENSE;
import static com.parcels.domain.enums.TransactionType.INCOME;

/**
 * Builds the monthly report for the whole household, with a per-person split. Totals are always per currency;
 * month boundaries use the application's local zone.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    static final String UNCATEGORIZED = "Uncategorized";
    private static final int TOP_MERCHANTS = 5;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;
    private final PersonService personService;
    private final ConversionService conversionService;
    private final Clock clock;

    public MonthlyReport monthly(YearMonth month) {
        List<Transaction> current = transactionsIn(month);
        List<Transaction> previous = transactionsIn(month.minusMonths(1));
        List<String> people = peopleIn(current);

        return new MonthlyReport(
                month,
                month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear(),
                people,
                OffsetDateTime.now(clock),
                currencySummaries(current, previous),
                personSummaries(current, people),
                categoryTotals(current, people),
                topMerchants(current),
                current.stream().map(t -> conversionService.convert(t, TransactionDto.class)).toList(),
                current.stream().anyMatch(t -> t.getCategories().size() > 1));
    }

    private List<Transaction> transactionsIn(YearMonth month) {
        return transactionRepository.findAllInRange(start(month), start(month.plusMonths(1)));
    }

    private OffsetDateTime start(YearMonth month) {
        ZoneId zone = clock.getZone();
        return month.atDay(1).atStartOfDay(zone).toOffsetDateTime();
    }

    /** Display names of the people with transactions, in household order (as configured). */
    private List<String> peopleIn(List<Transaction> transactions) {
        Set<Long> ids = transactions.stream().map(Transaction::getUserAccount).filter(Objects::nonNull)
                .map(UserAccount::getId).collect(Collectors.toSet());
        return personService.all().stream().filter(p -> ids.contains(p.getId())).map(UserAccount::displayName).toList();
    }

    private static String personOf(Transaction t) {
        return t.getUserAccount() == null ? "Unknown" : t.getUserAccount().displayName();
    }

    private static List<CurrencySummary> currencySummaries(List<Transaction> current, List<Transaction> previous) {
        List<CurrencySummary> result = new ArrayList<>();
        for (Currency currency : currencies(current, previous)) {
            BigDecimal income = sum(current, currency, INCOME);
            BigDecimal expense = sum(current, currency, EXPENSE);
            BigDecimal previousExpense = sum(previous, currency, EXPENSE);
            BigDecimal change = previousExpense.signum() == 0 ? null
                    : percent(expense.subtract(previousExpense), previousExpense);
            result.add(new CurrencySummary(currency, income, expense, income.subtract(expense),
                    sum(previous, currency, INCOME), previousExpense, change));
        }
        return result;
    }

    private static List<PersonSummary> personSummaries(List<Transaction> current, List<String> people) {
        List<PersonSummary> result = new ArrayList<>();
        for (String person : people) {
            List<Transaction> own = current.stream().filter(t -> personOf(t).equals(person)).toList();
            for (Currency currency : currencies(own, List.of())) {
                BigDecimal income = sum(own, currency, INCOME);
                BigDecimal expense = sum(own, currency, EXPENSE);
                result.add(new PersonSummary(person, currency, income, expense, income.subtract(expense)));
            }
        }
        return result;
    }

    /**
     * Expenses per currency and category, split by person. A transaction with several categories counts in full under each.
     */
    private static List<CategoryTotal> categoryTotals(List<Transaction> transactions, List<String> people) {
        record Key(Currency currency, String category) {
        }
        Map<Key, List<Transaction>> byKey = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.getTransactionType() != EXPENSE) {
                continue;
            }
            List<String> names = t.getCategories().isEmpty()
                    ? List.of(UNCATEGORIZED)
                    : t.getCategories().stream().map(Category::getName).toList();
            names.forEach(name -> byKey.computeIfAbsent(new Key(t.getCurrency(), name), k -> new ArrayList<>()).add(t));
        }
        Map<Currency, BigDecimal> expenseByCurrency = transactions.stream()
                .filter(t -> t.getTransactionType() == EXPENSE)
                .collect(Collectors.groupingBy(Transaction::getCurrency, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        return byKey.entrySet().stream()
                .map(e -> {
                    BigDecimal amount = total(e.getValue());
                    Map<String, BigDecimal> byPerson = new LinkedHashMap<>();
                    for (String person : people) {
                        BigDecimal own = total(e.getValue().stream().filter(t -> personOf(t).equals(person)).toList());
                        if (own.signum() != 0) {
                            byPerson.put(person, own);
                        }
                    }
                    return new CategoryTotal(e.getKey().currency(), e.getKey().category(), amount, e.getValue().size(),
                            percent(amount, expenseByCurrency.get(e.getKey().currency())), byPerson);
                })
                .sorted(Comparator.comparing(CategoryTotal::currency)
                        .thenComparing(CategoryTotal::amount, Comparator.reverseOrder())
                        .thenComparing(CategoryTotal::category))
                .toList();
    }

    private static List<MerchantTotal> topMerchants(List<Transaction> transactions) {
        Map<Currency, Map<String, List<Transaction>>> grouped = transactions.stream()
                .filter(t -> t.getTransactionType() == EXPENSE && t.getMerchant() != null)
                .collect(Collectors.groupingBy(Transaction::getCurrency, () -> new EnumMap<>(Currency.class),
                        Collectors.groupingBy(t -> t.getMerchant().trim())));
        List<MerchantTotal> result = new ArrayList<>();
        grouped.forEach((currency, byMerchant) -> byMerchant.entrySet().stream()
                .map(e -> new MerchantTotal(currency, e.getKey(), total(e.getValue()), e.getValue().size()))
                .sorted(Comparator.comparing(MerchantTotal::amount, Comparator.reverseOrder()).thenComparing(MerchantTotal::merchant))
                .limit(TOP_MERCHANTS)
                .forEach(result::add));
        return result;
    }

    private static Set<Currency> currencies(List<Transaction> a, List<Transaction> b) {
        Set<Currency> currencies = EnumSet.noneOf(Currency.class);
        a.forEach(t -> currencies.add(t.getCurrency()));
        b.forEach(t -> currencies.add(t.getCurrency()));
        currencies.remove(null);
        return currencies;
    }

    private static BigDecimal sum(List<Transaction> transactions, Currency currency, TransactionType type) {
        return total(transactions.stream().filter(t -> t.getCurrency() == currency && t.getTransactionType() == type).toList());
    }

    private static BigDecimal total(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
