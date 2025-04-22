package com.swarmer.finance.dto.dump;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swarmer.finance.dto.AccountDto;
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
                var dscale = transaction.getAccount() != null ? transaction.getAccount().getScale() : (transaction.getRecipient() != null ? transaction.getRecipient().getScale() : 2);
                var cscale = transaction.getRecipient() != null ? transaction.getRecipient().getScale() : dscale;
                var debit = AccountDto.setScale(transaction.getDebit(), dscale);
                var credit = AccountDto.setScale(transaction.getCredit(), cscale);
                return new DumpTransaction(
                                transaction.getId(),
                                transaction.getOpdate(),
                                transaction.getAccount() == null ? null : transaction.getAccount().getId(),
                                debit,
                                transaction.getRecipient() == null ? null : transaction.getRecipient().getId(),
                                credit,
                                transaction.getCategory() == null ? null : transaction.getCategory().getId(),
                                transaction.getCurrency(),
                                transaction.getParty(),
                                transaction.getDetails(),
                                transaction.getCreated(),
                                transaction.getUpdated());
        }
}
