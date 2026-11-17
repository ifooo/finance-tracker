package com.parcels.transaction.dto;

import java.math.BigDecimal;

public record TransactionItemDto(String name,
                                 BigDecimal quantity,
                                 BigDecimal unitPrice,
                                 BigDecimal total) {
}
