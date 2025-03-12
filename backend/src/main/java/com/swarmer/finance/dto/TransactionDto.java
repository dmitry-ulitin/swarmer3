package com.swarmer.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.models.TransactionType;

public record TransactionDto(
                Long id,
                Long ownerId,
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[XXX]") LocalDateTime opdate,
                TransactionType type,
                AccountDto account,
                BigDecimal debit,
                AccountDto recipient,
                BigDecimal credit,
                CategoryDto category,
                String currency,
                String party,
                String details) {
        public static TransactionDto fromEntity(Transaction entity, long userId, BigDecimal accauntBalance,
                        BigDecimal recipientBalance) {
                var account = AccountDto.fromEntity(entity.getAccount(), userId, accauntBalance,
                                entity.getOpdate());
                var recipient = AccountDto.fromEntity(entity.getRecipient(), userId, recipientBalance,
                                entity.getOpdate());
                var category = CategoryDto.fromEntity(entity.getCategory());
                var type = account != null && recipient != null ? TransactionType.TRANSFER
                                : (category == null
                                                ? (account == null ? TransactionType.INCOME : TransactionType.EXPENSE)
                                                : category.type());
                return new TransactionDto(entity.getId(), entity.getOwnerId(), entity.getOpdate(), type, account,
                                entity.getDebit(), recipient, entity.getCredit(), category, entity.getCurrency(),
                                entity.getParty(),
                                entity.getDetails());
        }
}
