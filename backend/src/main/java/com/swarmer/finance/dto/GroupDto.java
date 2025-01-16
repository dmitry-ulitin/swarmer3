package com.swarmer.finance.dto;

import java.util.List;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.AccountGroup;

public record GroupDto(
		Long id,
		Long ownerId,
		String ownerEmail,
		String fullName,
		@JsonProperty("is_owner") Boolean owner,
		@JsonProperty("is_coowner") Boolean coowner,
		@JsonProperty("is_shared") Boolean shared,
		List<AccountDto> accounts,
		List<Permission> permissions,
		LocalDateTime opdate,
		Boolean deleted) {
	public static GroupDto from(AccountGroup group, Long userId, List<TransactionSum> balances) {
		var owner = group.getOwner().getId().equals(userId)
				&& group.getAcls().stream().noneMatch(acl -> acl.getAdmin());
		var coowner = group.getAcls().stream().anyMatch(acl -> acl.getAdmin()
				&& (group.getOwner().getId().equals(userId) || acl.getUser().getId().equals(userId)));
		var shared = !group.getOwner().getId().equals(userId)
				&& group.getAcls().stream().noneMatch(acl -> acl.getAdmin());
		var fullName = group.getName();
		if (shared) {
			var acl = group.getAcls().stream().filter(a -> a.getUserId().equals(userId)).findFirst().orElse(null);
			fullName = acl != null && acl.getName() != null ? acl.getName()
					: (group.getName() + " (" + group.getOwner().getName() + ")");
		}
		var permissions = group.getAcls().stream().map(acl -> Permission.from(acl))
				.collect(java.util.stream.Collectors.toList());
		var accounts = group.getAccounts().stream()
				.map(account -> {
					var balance = balances.stream().filter(b -> account.getId().equals(b.getRecipientId()))
							.mapToDouble(a -> a.getCredit()).sum();
					balance -= balances.stream().filter(b -> account.getId().equals(b.getAccountId()))
							.mapToDouble(a -> a.getDebit()).sum();
					var opdate = balances.stream()
							.filter(b -> account.getId().equals(b.getAccountId()) || account.getId().equals(b.getRecipientId()))
							.map(TransactionSum::getOpdate).filter(o -> o != null).max(LocalDateTime::compareTo)
							.orElse(null);
					return AccountDto.from(account, userId, account.getStartBalance() + balance, opdate);
				})
				.sorted((a, b) -> a.id().compareTo(b.id()))
				.collect(java.util.stream.Collectors.toList());
		var opdate = group.getAccounts().stream()
				.map(account -> balances.stream()
						.filter(b -> account.getId().equals(b.getAccountId())
								|| account.getId().equals(b.getRecipientId()))
						.map(TransactionSum::getOpdate).filter(o -> o != null).max(LocalDateTime::compareTo)
						.orElse(null))
				.filter(o -> o != null)
				.max(LocalDateTime::compareTo).orElse(null);
		return new GroupDto(
				group.getId(),
				group.getOwner().getId(),
				group.getOwner().getEmail(),
				fullName,
				owner,
				coowner,
				shared,
				accounts,
				permissions,
				opdate,
				group.getDeleted());
	}
}
