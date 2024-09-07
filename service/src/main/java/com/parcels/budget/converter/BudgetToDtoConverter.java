package com.parcels.budget.converter;

import com.parcels.budget.dto.BudgetDto;
import com.parcels.domain.Budget;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class BudgetToDtoConverter implements Converter<Budget, BudgetDto> {

    @Override
    public BudgetDto convert(Budget source) {
        if (source == null) {
            return null;
        }
        return new BudgetDto(
                source.getId(),
                source.getUser() != null ? source.getUser().getFullName() : null,
                source.getCategory() != null ? source.getCategory().getName() : null,
                source.getAmount(),
                source.getStartDate(),
                source.getEndDate());
    }
}
