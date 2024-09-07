package com.parcels.transaction;

import com.parcels.domain.enums.TransactionType;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionService {
    TransactionDto getById(Long id);

    TransactionDto save(TransactionUpdateCommand transactionPersistCommand);

    Boolean delete(Long id);

    /**
     * dwfs
     *
     * @param transactionType
     * @param from
     * @param to
     * @param page
     * @param size
     * @return
     */
    Page<TransactionDto> getTransactionsByTypeAndDateRange(TransactionType transactionType,
                                                           OffsetDateTime from,
                                                           OffsetDateTime to,
                                                           int page,
                                                           int size);

    Page<TransactionDto> getTransactionsByDateRange(OffsetDateTime from,
                                                    OffsetDateTime to,
                                                    List<Long> userids,
                                                    int page,
                                                    int size);
}
