package com.swarmer.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.swarmer.finance.models.AccountGroup;

public record GroupDto(
        Long id,
        Long ownerId,
        String ownerEmail,
        String fullName,
        boolean owner,
        boolean coowner,
        boolean shared,
        boolean deleted,
        List<AccountDto> accounts,
        List<AclDto> permissions,
        LocalDateTime opdate) {
    public static GroupDto fromEntity(AccountGroup entity, Long userId, List<TransactionSum> balances) {
        var owner = entity.getOwner().getId().equals(userId)
                && entity.getAcls().stream().noneMatch(acl -> acl.isAdmin());
        var coowner = entity.getAcls().stream().anyMatch(acl -> acl.isAdmin()
                && (entity.getOwner().getId().equals(userId) || acl.getUser().getId().equals(userId)));
        var shared = !entity.getOwner().getId().equals(userId)
                && entity.getAcls().stream().noneMatch(
                        acl -> acl.getUser().getId().equals(userId) && acl.isAdmin());
        var fullname = entity.getName();
        if (shared) {
            var acl = entity.getAcls().stream().filter(a -> a.getUser().getId().equals(userId)).findFirst()
                    .orElse(null);
            fullname = acl != null && acl.getName() != null ? acl.getName()
                    : (entity.getName() + " (" + entity.getOwner().getName() + ")");
        }
        var acls = entity.getAcls().stream().map(acl -> AclDto.fromEntity(acl)).toList();
        var accounts = entity.getAccounts().stream()
                .map(a -> {
                    var debit = balances.stream().filter(b -> a.getId().equals(b.accountId()))
                            .map(b -> b.debit())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var credit = balances.stream().filter(b -> a.getId().equals(b.recipientId()))
                            .map(b -> b.credit())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var balance = a.getStartBalance().add(credit).subtract(debit);
                    var lastOpdate = balances.stream()
                            .filter(b -> a.getId().equals(b.accountId())
                                    || a.getId().equals(b.recipientId()))
                            .map(b -> b.opdate()).max(LocalDateTime::compareTo)
                            .orElse(null);
                    return AccountDto.fromEntity(a, userId, balance, lastOpdate);
                })
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
        LocalDateTime lastGroupOpdate = accounts.stream().map(a -> a.opdate()).filter(o -> o != null)
                .max(LocalDateTime::compareTo).orElse(null);
        return new GroupDto(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getOwner().getEmail(),
                fullname,
                owner,
                coowner,
                shared,
                entity.isDeleted(),
                accounts,
                acls,
                lastGroupOpdate);
    }
}