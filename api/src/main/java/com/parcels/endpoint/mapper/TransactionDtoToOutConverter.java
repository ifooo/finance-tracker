package com.parcels.endpoint.mapper;

import com.parcels.endpoint.dto.out.TransactionOut;
import com.parcels.transaction.dto.TransactionDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TransactionDtoToOutConverter implements Converter<TransactionDto, TransactionOut> {
    @Override
    public TransactionOut convert(TransactionDto source) {
        if (source == null) {
            return null;
        }
        return new TransactionOut(source.id(),
                source.transactionType(),
                source.amount(),
                source.currency(),
                source.description(),
                source.category() != null ? source.category() : "",
                source.dateFrom());
    }
}
