package com.parcels.endpoint.rest;

import com.parcels.budget.BudgetService;
import com.parcels.budget.dto.BudgetDto;
import com.parcels.budget.dto.BudgetUpdateCommand;
import com.parcels.endpoint.dto.out.BudgetOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BudgetResource.API_PATH)
@RequiredArgsConstructor
@Slf4j
public class BudgetResource {
    public static final String API_PATH = "/api/v1/budgets";

    private final BudgetService budgetService;
    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<BudgetOut> addBudget(@RequestBody BudgetUpdateCommand budgetUpdateCommand) {
        final BudgetDto resultDto = budgetService.save(budgetUpdateCommand);
        final BudgetOut convertedResult = conversionService.convert(resultDto, BudgetOut.class);
        return ResponseEntity.ok(convertedResult);
    }
}
