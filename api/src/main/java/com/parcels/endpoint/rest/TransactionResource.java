package com.parcels.endpoint.rest;

import com.parcels.domain.enums.TransactionType;
import com.parcels.endpoint.dto.PagedResponse;
import com.parcels.endpoint.dto.out.TransactionOut;
import com.parcels.endpoint.mapper.PageToPagedResponseOutConverter;
import com.parcels.transaction.TransactionService;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(maxAge = 3600, origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TransactionResource {

    private final TransactionService transactionService;
    private final ConversionService conversionService;
    private final PageToPagedResponseOutConverter<TransactionOut> pageToPagedResponseOutConverter;

    // add income
    @PostMapping
    public ResponseEntity<TransactionOut> addTransaction(@Valid @RequestBody TransactionUpdateCommand transactionPersistCommand) {
        TransactionDto result = transactionService.save(transactionPersistCommand);
        TransactionOut convertedResult = conversionService.convert(result, TransactionOut.class);
        return ResponseEntity.ok(convertedResult);
    }

    // delete income

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteTransaction(@PathVariable(name = "id") Long id) {
        Boolean b = transactionService.delete(id);
        return ResponseEntity.ok(b);
    }

    // get particular income

    // get paginated incomes by date range
    @GetMapping
    public PagedResponse<TransactionOut> getTransactions(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                         @RequestParam(name = "size", required = false, defaultValue = "10") int size,
                                                         @RequestParam(name = "type") TransactionType transactionType,
                                                         @RequestParam(name = "from", required = false) OffsetDateTime from,
                                                         @RequestParam(name = "to", required = false) OffsetDateTime to) {
        Page<TransactionDto> pagedTransactionsByTypeAndDateRange = transactionService.getTransactionsByTypeAndDateRange(
                transactionType,
                from,
                to,
                page,
                size);

        final var result = pagedTransactionsByTypeAndDateRange.map(transactionDto -> conversionService.convert(transactionDto, TransactionOut.class));
        final var convertedResult = pageToPagedResponseOutConverter.convert(result);
        return convertedResult;
    }

}
