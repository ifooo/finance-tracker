package com.parcels.mail;

import com.parcels.transaction.dto.TransactionDto;
import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface EmailService {
    void sendMonthlyOverviewEmail(String[] recipients, String subject, List<TransactionDto> transactions)
            throws MessagingException, UnsupportedEncodingException;
}
