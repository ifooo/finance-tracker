package com.parcels.endpoint.rest;

import com.parcels.domain.enums.TransactionType;
import com.parcels.endpoint.dto.PagedResponse;
import com.parcels.endpoint.dto.out.TransactionOut;
import com.parcels.endpoint.mapper.PageToPagedResponseOutConverter;
import com.parcels.transaction.TransactionService;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "Endpoints for managing transactions")
public class TransactionResource {

    private final TransactionService transactionService;
    private final ConversionService conversionService;
    private final PageToPagedResponseOutConverter<TransactionOut> pageToPagedResponseOutConverter;

    @PostMapping
    @Operation(summary = "Add a new transaction",
            description = "Creates a new transaction record.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = TransactionUpdateCommand.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction created successfully",
                            content = @Content(schema = @Schema(implementation = TransactionOut.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            })
    public ResponseEntity<TransactionOut> addTransaction(
            @Valid @RequestBody TransactionUpdateCommand transactionPersistCommand) {
        TransactionDto result = transactionService.save(transactionPersistCommand);
        TransactionOut convertedResult = conversionService.convert(result, TransactionOut.class);
        return ResponseEntity.ok(convertedResult);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction",
            description = "Deletes a transaction record by its ID.",
            parameters = @Parameter(name = "id", description = "ID of the transaction to be deleted", required = true, schema = @Schema(type = "integer")),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction deleted successfully",
                            content = @Content(schema = @Schema(type = "boolean"))),
                    @ApiResponse(responseCode = "404", description = "Transaction not found")
            })
    public ResponseEntity<Boolean> deleteTransaction(@PathVariable(name = "id") Long id) {
        Boolean b = transactionService.delete(id);
        return ResponseEntity.ok(b);
    }

    @GetMapping
    @Operation(summary = "Get transactions",
            description = "Retrieves a paginated list of transactions filtered by type and date range.",
            parameters = {
                    @Parameter(name = "page", description = "Page number for pagination", schema = @Schema(type = "integer", defaultValue = "0")),
                    @Parameter(name = "size", description = "Number of transactions per page", schema = @Schema(type = "integer", defaultValue = "10")),
                    @Parameter(name = "type", description = "Type of transactions to retrieve", required = true, schema = @Schema(type = "string")),
                    @Parameter(name = "from", description = "Start date for filtering transactions", schema = @Schema(type = "string", format = "date-time")),
                    @Parameter(name = "to", description = "End date for filtering transactions", schema = @Schema(type = "string", format = "date-time"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated list of transactions retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PagedResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input parameters")
            })
    public PagedResponse<TransactionOut> getTransactions(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
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

