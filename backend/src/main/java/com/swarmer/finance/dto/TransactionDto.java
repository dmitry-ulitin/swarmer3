package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.models.TransactionType;

public record TransactionDto(
    Long id,
    Long ownerId,
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[XXX]")
    LocalDateTime opdate,
    TransactionType type,
	AccountDto account,
    Double debit,
	AccountDto recipient,
    Double credit,
	Category category,
    String currency,
    String party,
    String details
) {
    public static TransactionDto from(Transaction transaction, Long userId, Double accountBalance, Double recipientBalance) {
        var account = transaction.getAccount() == null ? null : AccountDto.from(transaction.getAccount(), userId, accountBalance, transaction.getOpdate());
        var recipient = transaction.getRecipient() == null ? null : AccountDto.from(transaction.getRecipient(), userId, recipientBalance, transaction.getOpdate());
        return new TransactionDto(
            transaction.getId(),
            transaction.getOwner().getId(),
            transaction.getOpdate(),
            account != null && recipient != null ? TransactionType.TRANSFER : (transaction.getCategory() != null && transaction.getCategory().getId() == TransactionType.CORRECTION.getValue() ? TransactionType.CORRECTION : (account != null ? TransactionType.EXPENSE : TransactionType.INCOME)),
            account,
            transaction.getDebit(),
            recipient,
            transaction.getCredit(),
            transaction.getCategory(),
            transaction.getCurrency(),
            transaction.getParty(),
            transaction.getDetails()
        );
    }
}
