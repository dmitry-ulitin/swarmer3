package com.swarmer.finance.dto.dump;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swarmer.finance.dto.AccountDto;
import com.swarmer.finance.models.Account;

public record DumpAccount(
    Long id,
    String name,
    String currency,
    BigDecimal startBalance,
    String chain,
    String address,
    Integer scale,
    Boolean deleted,
    LocalDateTime created,
    LocalDateTime updated) {
public static DumpAccount fromEntity(Account account) {
    return new DumpAccount(
                    account.getId(),
                    account.getName(),
                    account.getCurrency(),
                    AccountDto.setScale(account.getStartBalance(), account.getScale()),
                    account.getChain(),
                    account.getAddress(),
                    account.getScale(),
                    account.isDeleted(),
                    account.getCreated(),
                    account.getUpdated());
}
}