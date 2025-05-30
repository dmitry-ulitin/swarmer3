package com.swarmer.finance.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.swarmer.finance.models.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AccountDto(
        Long id,
        String name,
        String fullName,
        String currency,
        BigDecimal startBalance,
        String chain,
        String address,
        Integer scale,
        BigDecimal balance,
        LocalDateTime opdate,
        boolean deleted) {
    private static final Map<Integer, BigDecimal> DECIMALS = Map.of(
            0, BigDecimal.valueOf(1),
            2, BigDecimal.valueOf(100),
            6, BigDecimal.valueOf(1000000),
            8, BigDecimal.valueOf(100000000),
            18, BigDecimal.valueOf(1000000000000000000L)
    );

    public static AccountDto fromEntity(Account entity, Long userId, BigDecimal balance, LocalDateTime opdate) {
        if (entity == null) {
            return null;
        }
        var fullname = getFullName(entity);
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
                setScale(entity.getStartBalance(), entity.getScale()),
                entity.getChain(),
                entity.getAddress(),
                entity.getScale(),
                setScale(balance, entity.getScale()),
                opdate,
                entity.isDeleted());
    }

    public static String getFullName(Account entity) {
        if (entity == null) {
            return null;
        }
        var fullname = entity.getGroup().getName();
        if (entity.getName() != null && !entity.getName().isBlank()) {
            fullname += " " + entity.getName();
        } else if (entity.getGroup().getAccounts().stream().filter(a -> !a.isDeleted()).count() > 1) {
            fullname += " " + entity.getCurrency();
        }
        return fullname;
    }

    public static BigDecimal setScale(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        var decimals = DECIMALS.getOrDefault(scale, BigDecimal.valueOf(1));
        return value.divide(decimals, scale, RoundingMode.HALF_DOWN);
    }
    
    public static BigDecimal unsetScale(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        var decimals = DECIMALS.getOrDefault(scale, BigDecimal.valueOf(1));
        return value.multiply(decimals).setScale(0, RoundingMode.HALF_DOWN);
    }
}