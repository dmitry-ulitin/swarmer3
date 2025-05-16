package com.swarmer.finance.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.AccountDto;
import com.swarmer.finance.dto.GroupDto;
import com.swarmer.finance.exceptions.ResourceNotFoundException;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.AccountGroup;
import com.swarmer.finance.models.Acl;
import com.swarmer.finance.models.AclId;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AclRepository;
import com.swarmer.finance.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class GroupService {
    private final AccountGroupRepository groupRepository;
    private final AclRepository aclRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public GroupService(AccountGroupRepository groupRepository, AclRepository aclRepository,
            UserRepository userRepository, TransactionService transactionService) {
        this.groupRepository = groupRepository;
        this.aclRepository = aclRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public List<GroupDto> getGroups(Long userId, LocalDateTime opdate) {
        var userGroups = groupRepository.findByOwnerIdOrderById(userId);
        var sharedGroups = aclRepository.findByUserIdOrderByGroupId(userId).stream()
                .map(acl -> acl.getGroup())
                .filter(group -> !group.getOwner().getId().equals(userId))
                .toList();
        var accList = Stream.concat(userGroups.stream(), sharedGroups.stream())
                .flatMap(group -> group.getAccounts().stream())
                .map(account -> account.getId())
                .toList();
        var balances = transactionService.getBalances(accList, null, opdate, null);
        var allGroups = userGroups.stream().map(g -> GroupDto.fromEntity(g, userId, balances))
                .filter(GroupDto::owner).collect(Collectors.toList());
        allGroups.addAll(userGroups.stream()
                .map(g -> GroupDto.fromEntity(g, userId, balances))
                .filter(GroupDto::coowner)
                .toList());
        allGroups.addAll(sharedGroups.stream()
                .map(g -> GroupDto.fromEntity(g, userId, balances))
                .filter(GroupDto::coowner)
                .toList());
        allGroups.addAll(sharedGroups.stream()
                .map(g -> GroupDto.fromEntity(g, userId, balances))
                .filter(GroupDto::shared)
                .toList());
        return allGroups;
    }

    @Transactional
    public GroupDto getGroup(Long groupId, Long userId) {
        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + groupId));
        var accList = group.getAccounts().stream().map(account -> account.getId()).toList();
        var balances = transactionService.getBalances(accList, null, null, null);
        return GroupDto.fromEntity(group, userId, balances);
    }

    @Transactional
    public GroupDto createGroup(GroupDto dto, Long userId) {
        var group = new AccountGroup();
        group.setOwner(userRepository.findById(userId).orElseThrow());
        group.setName(dto.fullName());
        group.setAccounts(dto.accounts().stream().map(a -> {
            var account = new Account();
            account.setGroup(group);
            account.setName(a.name());
            account.setCurrency(a.currency());
            account.setStartBalance(AccountDto.unsetScale(a.startBalance(), a.scale()));
            account.setChain(a.chain());
            account.setAddress(a.address());
            account.setScale(a.scale());
            return account;
        }).toList());
        group.setAcls(dto.permissions().stream().map(p -> {
            var acl = new Acl();
            acl.setGroup(group);
            acl.setUser(userRepository.findByEmailIgnoreCase(p.user().email()).orElseThrow(
                    () -> new UsernameNotFoundException("User not found with email: " + p.user().email())));
            acl.setId(new AclId(group.getId(), acl.getUser().getId()));
            acl.setAdmin(p.admin());
            acl.setReadonly(p.readonly() && !p.admin());
            return acl;
        }).toList());
        groupRepository.save(group);
        return GroupDto.fromEntity(group, userId, List.of());
    }

    @Transactional
    public GroupDto updateGroup(GroupDto dto, boolean force, Long userId) {
        var group = groupRepository.findById(dto.id()).orElseThrow();
        var admin = true;
        if (!group.getOwner().getId().equals(userId)) {
            var acl = group.getAcls().stream().filter(a -> a.getUser().getId().equals(userId)).findFirst().orElse(null);
            if (acl == null || acl.isReadonly()) {
                throw new RuntimeException("Not owner");
            }
            admin = acl.isAdmin();
            var fullName = admin ? group.getName() : (group.getName() + " (" + acl.getUser().getName() + ")");
            acl.setName(dto.fullName().equals(fullName) ? null : dto.fullName());
        } else {
            group.setName(dto.fullName());
        }
        var accList = group.getAccounts().stream().map(account -> account.getId()).toList();
        var balances = transactionService.getBalances(accList, null, null, null);
        if (admin) {
            dto.accounts().forEach(a -> {
                if (a.id() != null && a.id() != 0) {
                    var account = group.getAccounts().stream().filter(acc -> acc.getId().equals(a.id()))
                            .findFirst().orElseThrow();
                    if (a.deleted() && balances.stream()
                            .noneMatch(b -> a.id().equals(b.accountId()) || a.id().equals(b.recipientId()))) {
                        group.getAccounts().remove(account);
                    } else {
                        if (a.deleted()) {
                            // Check if account has non-zero balance
                            var debit = balances.stream().filter(b -> a.id().equals(b.accountId()))
                                    .map(b -> b.debit())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            var credit = balances.stream().filter(b -> a.id().equals(b.recipientId()))
                                    .map(b -> b.credit())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            var balance = a.startBalance().add(credit).subtract(debit);
                            if (!balance.equals(BigDecimal.ZERO)) {
                                if (force) {
                                    // Force delete all transactions for this account
                                    transactionService.deleteTransactionsByAccounts(List.of(a.id()));
                                    group.getAccounts().remove(account);
                                } else {
                                    throw new RuntimeException("Account '" + a.fullName() + "' has non-zero balance");
                                }
                            }
                        }
                        account.setName(a.name());
                        account.setCurrency(a.currency());
                        account.setStartBalance(AccountDto.unsetScale(a.startBalance(), a.scale()));
                        account.setChain(a.chain());
                        account.setAddress(a.address());
                        account.setScale(a.scale());
                        account.setDeleted(a.deleted());
                    }
                } else if (!a.deleted()) {
                    var account = new Account();
                    account.setGroup(group);
                    account.setName(a.name());
                    account.setCurrency(a.currency());
                    account.setStartBalance(AccountDto.unsetScale(a.startBalance(), a.scale()));
                    account.setChain(a.chain());
                    account.setAddress(a.address());
                    account.setScale(a.scale());
                    group.getAccounts().add(account);
                }
            });

            group.getAcls().stream().toList().forEach(acl -> {
                var p = dto.permissions().stream()
                        .filter(p1 -> p1.user().email().equalsIgnoreCase(acl.getUser().getEmail()))
                        .findFirst().orElse(null);
                if (p == null) {
                    aclRepository.delete(acl);
                    group.getAcls().remove(acl);
                } else {
                    acl.setAdmin(p.admin());
                    acl.setReadonly(p.readonly() && !p.admin());
                    acl.setUpdated(LocalDateTime.now());
                    dto.permissions().remove(p);
                }
            });
            dto.permissions().forEach(p -> {
                var acl = new Acl();
                acl.setUser(userRepository.findByEmailIgnoreCase(p.user().email()).orElseThrow());
                if (!acl.getUser().getId().equals(userId)) {
                    acl.setId(new AclId(group.getId(), acl.getUser().getId()));
                    acl.setGroup(group);
                    acl.setAdmin(p.admin());
                    acl.setReadonly(p.readonly() && !p.admin());
                    group.getAcls().add(acl);
                }
            });
        }
        group.setUpdated(LocalDateTime.now());
        return GroupDto.fromEntity(groupRepository.save(group), userId, balances);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId, boolean force) {
        var group = groupRepository.findById(groupId).orElseThrow();
        if (!group.getOwner().getId().equals(userId)) {
            var acl = group.getAcls().stream().filter(a -> a.getUser().getId().equals(userId)).findFirst().orElse(null);
            if (acl == null || !acl.isAdmin()) {
                throw new RuntimeException("Not owner");
            }
        }
        var accList = group.getAccounts().stream().map(account -> account.getId()).toList();
        var balances = transactionService.getBalances(accList, null, null, null);
        if (balances.isEmpty()) {
            groupRepository.delete(group);
            return;
        }
        var dto = GroupDto.fromEntity(group, userId, balances);
        var isEmpty = dto.accounts().stream()
                .allMatch(a -> a.balance().equals(BigDecimal.ZERO) && !a.deleted());
        if (!force && !isEmpty) {
            throw new RuntimeException("This group has accounts with non-zero balance");
        }
        if (force && !isEmpty) {
            // Force delete all transactions for this group
            transactionService.deleteTransactionsByAccounts(accList);
            // Delete group
            groupRepository.delete(group);
            return;
        }
        group.setDeleted(true);
        group.setUpdated(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Transactional
    public List<String> findUsers(String query) {
        return userRepository.findFirst10ByEmailnameContainingIgnoreCase(query).stream().map(u -> u.getEmail())
                .toList();
    }
}
