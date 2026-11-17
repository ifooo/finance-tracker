package com.parcels.transaction.converter;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionItemDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TransactionToDtoConverter implements Converter<Transaction, TransactionDto> {
    @Override
    public TransactionDto convert(Transaction source) {
        if (source == null) {
            return null;
        }
        return new TransactionDto(source.getId(),
                source.getTransactionType(),
                source.getAmount(),
                source.getCurrency(),
                source.getDescription(),
                source.getCategories().stream().map(Category::getId).toList(),
                source.getCategories().stream().map(Category::getName).toList(),
                source.getDateFrom(),
                source.getMerchant(),
                source.getPaymentMethod(),
                source.getNotes(),
                source.getSource(),
                source.getItems().stream()
                        .map(i -> new TransactionItemDto(i.getName(), i.getQuantity(), i.getUnitPrice(), i.getTotal()))
                        .toList(),
                source.getUserAccount() == null ? null : source.getUserAccount().getUsername(),
                source.getUserAccount() == null ? null : source.getUserAccount().displayName());
    }
}
