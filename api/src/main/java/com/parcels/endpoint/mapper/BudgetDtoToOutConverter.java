package com.parcels.endpoint.mapper;

import com.parcels.budget.dto.BudgetDto;
import com.parcels.endpoint.dto.out.BudgetOut;
import org.springframework.core.convert.converter.Converter;

public class BudgetDtoToOutConverter implements Converter<BudgetDto, BudgetOut> {

    @Override
    public BudgetOut convert(BudgetDto source) {
        if (source == null) {
            return null;
        }
        return new BudgetOut(
                source.id(),
                source.user(),
                source.categoryName(),
                source.amount(),
                source.startDate(),
                source.endDate());
    }
}
