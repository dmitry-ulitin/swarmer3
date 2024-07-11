package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.Account;

public record AccountDto(
        Long id,
        String name,
        String fullname,
        String currency,
        Double balance,
        @JsonProperty("start_balance") Double startBalance,
        LocalDateTime opdate,
        Boolean deleted) {
    public static AccountDto from(Account account, Long userId, Double balance, LocalDateTime opdate) {
        var accfullname = account.getGroup().getName();
        if (account.getName() != null && !account.getName().isBlank()) {
            accfullname += " " + account.getName();
        } else if (account.getGroup().getAccounts().stream().filter(a -> a.getDeleted() == null || !a.getDeleted()).count() > 1) {
            accfullname += " " + account.getCurrency();
        }
		var shared = !account.getGroup().getOwner().getId().equals(userId) && account.getGroup().getAcls().stream().noneMatch(acl -> acl.getAdmin());		
        if (shared) {
            accfullname += " (" + account.getGroup().getOwner().getName() + ")";
        }
        return new AccountDto(
                account.getId(),
                account.getName(),
                accfullname,
                account.getCurrency(),
                balance,
                account.getStart_balance(),
                opdate,
                account.getDeleted());
    }
}
