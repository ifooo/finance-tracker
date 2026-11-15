package com.parcels.mail;

import com.parcels.domain.enums.TransactionType;
import com.parcels.transaction.dto.TransactionDto;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.parcels.domain.enums.TransactionType.EXPENSE;
import static com.parcels.domain.enums.TransactionType.INCOME;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final Configuration configuration;

    @Override
    public void sendMonthlyOverviewEmail(String[] recipients, String subject, List<TransactionDto> transactions) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("shurbanovski.i@gmail.com", "Ivo Shurbanovski");
        helper.setTo(recipients);
        helper.setSubject(subject);
        helper.setText(renderMonthlyOverview(transactions), true);

        mailSender.send(message);
    }

    private static BigDecimal sumByType(List<TransactionDto> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.transactionType() == type)
                .map(TransactionDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // package-private for tests
    String renderMonthlyOverview(List<TransactionDto> transactions) {

        BigDecimal incomeSum = sumByType(transactions, INCOME);
        BigDecimal expenseSum = sumByType(transactions, EXPENSE);

        StringWriter stringWriter = new StringWriter();
        Map<String, Object> model = new HashMap<>();
        model.put("transactions", transactions);
        model.put("balance", incomeSum.subtract(expenseSum));

        try {
            configuration.getTemplate("monthly-overview.ftl").process(model, stringWriter);
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.getBuffer().toString();
    }
}