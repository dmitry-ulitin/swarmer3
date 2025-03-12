package com.swarmer.finance.dto.dump;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swarmer.finance.models.Transaction;

public record DumpTransaction(
                Long id,
                LocalDateTime opdate,
                Long accountId,
                BigDecimal debit,
                Long recipientId,
                BigDecimal credit,
                Long categoryId,
                String currency,
                String party,
                String details,
                LocalDateTime created,
                LocalDateTime updated) {
        public static DumpTransaction fromEntity(Transaction transaction) {
                return new DumpTransaction(
                                transaction.getId(),
                                transaction.getOpdate(),
                                transaction.getAccount() == null ? null : transaction.getAccount().getId(),
                                transaction.getDebit(),
                                transaction.getRecipient() == null ? null : transaction.getRecipient().getId(),
                                transaction.getCredit(),
                                transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                                transaction.getCurrency(),
                                transaction.getParty(),
                                transaction.getDetails(),
                                transaction.getCreated(),
                                transaction.getUpdated());
        }
}
