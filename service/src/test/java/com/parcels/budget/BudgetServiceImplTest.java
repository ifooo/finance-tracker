package com.parcels.budget;

import com.parcels.budget.dto.BudgetDto;
import com.parcels.budget.dto.BudgetUpdateCommand;
import com.parcels.domain.Category;
import com.parcels.domain.enums.Period;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.repository.CategoryRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetServiceImplTest extends BaseDbTest {

    private static final OffsetDateTime NOV_1 = OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        categoryId = categoryRepository.save(Category.builder().name("Groceries").build()).getId();
    }

    @Test
    void weeklyBudgetEndsAfterOneWeek() {
        assertThat(save(NOV_1, Period.WEEKLY).endDate()).isEqualTo(NOV_1.plusWeeks(1));
    }

    @Test
    void monthlyBudgetEndsAfterOneMonth() {
        assertThat(save(NOV_1, Period.MONTHLY).endDate()).isEqualTo(NOV_1.plusMonths(1));
    }

    @Test
    void yearlyBudgetEndsAfterOneYear() {
        assertThat(save(NOV_1, Period.YEARLY).endDate()).isEqualTo(NOV_1.plusYears(1));
    }

    @Test
    void rejectsMissingStartDate() {
        assertThatThrownBy(() -> save(null, Period.WEEKLY))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("startDate");
    }

    @Test
    void rejectsMissingCategory() {
        assertThatThrownBy(() -> budgetService.save(new BudgetUpdateCommand(null, null, null, BigDecimal.TEN, NOV_1, Period.WEEKLY)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("categoryId");
    }

    @Test
    void rejectsMissingPeriod() {
        assertThatThrownBy(() -> save(NOV_1, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("period");
    }

    private BudgetDto save(OffsetDateTime startDate, Period period) {
        return budgetService.save(new BudgetUpdateCommand(null, null, categoryId, new BigDecimal("100.00"), startDate, period));
    }
}
