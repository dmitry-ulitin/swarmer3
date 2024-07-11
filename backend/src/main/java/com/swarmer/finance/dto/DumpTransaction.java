package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.Transaction;

public record DumpTransaction(
        Long id,
        LocalDateTime opdate,
        @JsonProperty("account_id") Long accountId,
        double debit,
        @JsonProperty("recipient_id") Long recipientId,
        double credit,
        @JsonProperty("category_id") Long categoryId,
        String currency,
        String party,
        String details,
        LocalDateTime created,
        LocalDateTime updated) {
    public static DumpTransaction from(Transaction transaction) {
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
