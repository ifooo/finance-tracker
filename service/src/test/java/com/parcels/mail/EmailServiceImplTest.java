package com.parcels.mail;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.transaction.dto.TransactionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceImplTest extends BaseDbTest {

    private static final OffsetDateTime DATE = OffsetDateTime.of(2024, 11, 5, 18, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private EmailServiceImpl emailService;

    @Test
    void rendersCategoryNamesAndExactBalance() {
        List<TransactionDto> transactions = List.of(
                transaction(TransactionType.INCOME, "1000.10", List.of("Salary")),
                transaction(TransactionType.EXPENSE, "0.10", List.of("Food", "Fun")),
                transaction(TransactionType.EXPENSE, "0.20", List.of()));

        String html = emailService.renderMonthlyOverview(transactions);

        assertThat(html).contains("Salary").contains("Food, Fun");
        assertThat(html).contains(">2024-11-05<").contains("1000.10").contains("INCOME");
        assertThat(html).contains("Your balance for this month is 999.80 MKD.");
    }

    private static TransactionDto transaction(TransactionType type, String amount, List<String> categoryNames) {
        return new TransactionDto(1L, type, new BigDecimal(amount), Currency.MKD, "desc",
                List.of(), categoryNames, DATE);
    }
}
