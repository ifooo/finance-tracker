package com.parcels.budget;

import com.parcels.budget.dto.BudgetDto;
import com.parcels.budget.dto.BudgetUpdateCommand;
import com.parcels.domain.Budget;
import com.parcels.domain.Category;
import com.parcels.repository.BudgetRepository;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository repository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ConversionService conversionService;

    @Override
    public BudgetDto save(BudgetUpdateCommand budgetPersistCommand) {
        // category_id is NOT NULL in the schema, so a category is required too.
        if (budgetPersistCommand.categoryId() == null
                || budgetPersistCommand.startDate() == null
                || budgetPersistCommand.period() == null) {
            throw new ValidationException("Budget 'categoryId', 'startDate' and 'period' are required");
        }
        final Budget budget = new Budget();
        budget.setAmount(budgetPersistCommand.amount());
        Long categoryId = budgetPersistCommand.categoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category " + categoryId + " not found"));
        budget.setCategory(category);
        budget.setUser(userAccountRepository.findById(1L).orElseThrow());
        budget.setStartDate(budgetPersistCommand.startDate());
        budget.setPeriod(budgetPersistCommand.period());
        budget.setEndDate(budgetPersistCommand.period().endDateFrom(budgetPersistCommand.startDate()));
        Budget saved = repository.save(budget);
        BudgetDto result = conversionService.convert(saved, BudgetDto.class);
        return result;
    }

    @Override
    public Boolean delete(Long id) {
        Budget budget = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget " + id + " not found"));
        repository.delete(budget);
        return true;
    }
}
