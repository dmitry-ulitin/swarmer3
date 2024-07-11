package com.swarmer.finance.dto;

import java.time.LocalDateTime;

public record DumpAccount(
        Long id,
        String name,
        String currency,
        Double start_balance,
        Boolean deleted,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static DumpAccount from(com.swarmer.finance.models.Account account) {
        return new DumpAccount(
                account.getId(),
                account.getName(),
                account.getCurrency(),
                account.getStart_balance(),
                account.getDeleted(),
                account.getCreated(),
                account.getUpdated()
        );
    }
}
