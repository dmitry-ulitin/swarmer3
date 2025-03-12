package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.swarmer.finance.models.Account;

import java.math.BigDecimal;

public record AccountDto(
        Long id,
        String name,
        String fullName,
        String currency,
        BigDecimal startBalance,
        BigDecimal balance,
        LocalDateTime opdate,
        boolean deleted) {
    public static AccountDto fromEntity(Account entity, Long userId, BigDecimal balance, LocalDateTime opdate) {
        if (entity == null) {
            return null;
        }
        var fullname = entity.getGroup().getName();
        if (entity.getName() != null && !entity.getName().isBlank()) {
            fullname += " " + entity.getName();
        } else if (entity.getGroup().getAccounts().stream().filter(a -> !a.isDeleted()).count() > 1) {
            fullname += " " + entity.getCurrency();
        }
        var shared = !entity.getGroup().getOwner().getId().equals(userId) && entity.getGroup().getAcls().stream()
                .noneMatch(acl -> acl.getUser().getId().equals(userId) && acl.isAdmin());
        if (shared) {
            fullname += " (" + entity.getGroup().getOwner().getName() + ")";
        }
        return new AccountDto(
                entity.getId(),
                entity.getName(),
                fullname,
                entity.getCurrency(),
                entity.getStartBalance(),
                balance,
                opdate,
                entity.isDeleted());
    }
}