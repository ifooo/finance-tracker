package com.parcels.budget;

import com.parcels.budget.dto.BudgetDto;
import com.parcels.budget.dto.BudgetUpdateCommand;

public interface BudgetService {
    BudgetDto save(BudgetUpdateCommand budgetPersistCommand);
    Boolean delete(Long id);

}
