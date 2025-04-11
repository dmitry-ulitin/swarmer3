package com.swarmer.finance.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.CategoryDto;
import com.swarmer.finance.dto.dump.Dump;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.AccountGroup;
import com.swarmer.finance.models.Acl;
import com.swarmer.finance.models.AclId;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Rule;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AccountRepository;
import com.swarmer.finance.repositories.CategoryRepository;
import com.swarmer.finance.repositories.RuleRepository;
import com.swarmer.finance.repositories.TransactionRepository;
import com.swarmer.finance.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class BackupService {
    private final AccountGroupRepository groupRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final UserRepository userRepository;

    public BackupService(AccountGroupRepository groupRepository, AccountRepository accountRepository,
            CategoryRepository categoryRepository, TransactionRepository transactionRepository,
            RuleRepository ruleRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Dump getDump(Long userId) {
        var groups = groupRepository.findByOwnerIdOrderById(userId);
        var transactions = transactionRepository.findAllByOwnerId(userId);
        var rules = ruleRepository.findAllByOwnerId(userId);
        var categories = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(userId)).stream()
                .filter(c -> c.getOwnerId() != null)
                .sorted((a, b) -> CategoryDto.fromEntity(a).level() - CategoryDto.fromEntity(b).level())
                .toList();
        return Dump.fromEntities(userId, groups, categories, transactions, rules);
    }

    @Transactional
    public void loadDump(Long userId, Dump dump, boolean skipInvalidData) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id " + userId));
        var userGroups = groupRepository.findByOwnerIdOrderById(userId);
        var userAccounts = userGroups.stream()
                .flatMap(group -> group.getAccounts().stream())
                .collect(Collectors.toMap(Account::getId, a -> a));
        // clear existing data
        transactionRepository.deleteAllByOwnerId(userId);
        ruleRepository.deleteAllByOwnerId(userId);
        categoryRepository.deleteAllByOwnerId(userId);
        // groups
        var accMap = new HashMap<Long, Account>();
        for (var g : dump.groups()) {
            var group = new AccountGroup(null, user, g.name(), g.deleted(), new ArrayList<>(), new ArrayList<>(),
                    g.created(), g.updated());
            groupRepository.save(group);
            for (var a : g.accounts()) {
                var account = userAccounts.get(a.id());
                if (account == null) {
                    account = new Account(null, group, a.name(), a.currency(), a.startBalance(), a.chain(), a.address(), a.deleted(),
                            a.created(), a.updated());
                } else {
                    userAccounts.remove(a.id());
                    account.setName(a.name());
                    account.setCurrency(a.currency());
                    account.setStartBalance(a.startBalance());
                    account.setDeleted(a.deleted());
                    account.setUpdated(a.updated());
                    account.getGroup().getAccounts().remove(account);
                    account.setGroup(group);
                }

                group.getAccounts().add(account);
                groupRepository.save(group);
                account = group.getAccounts().getLast();
                accMap.put(a.id(), account);
            }
            for (var a : g.acls()) {
                var auser = userRepository.findById(a.userId()).orElse(null);
                if (auser != null) {
                    var acl = new Acl(new AclId(group.getId(), a.userId()), group, auser, a.readonly(), a.admin(),
                            a.name(), a.created(), a.updated());
                    group.getAcls().add(acl);
                }
            }
            groupRepository.save(group);
        }
        if (!userAccounts.isEmpty()) {
            // check if accounts are empty
            var transactions = transactionRepository.findByAccountIdInOrRecipientIdIn(userAccounts.keySet(),
                    userAccounts.keySet());
            if (!transactions.isEmpty()) {
                throw new RuntimeException("Accounts are not empty");
            }
        }
        for (var group : userGroups) {
            group.getAccounts().clear();
            groupRepository.delete(group);
        }

        // categories
        var catMap = new HashMap<Long, Category>();
        for (var c : dump.categories()) {
            var parent = catMap.getOrDefault(c.parentId(), categoryRepository.findById(c.parentId()).orElse(null));
            if (parent == null && c.parentId() != null) {
                throw new RuntimeException("Parent category not found: " + c.parentId());
            }
            var category = new Category(null, userId, parent, c.name(), c.created(), c.updated());
            categoryRepository.save(category);
            catMap.put(c.id(), category);
        }
        // transactions
        for (var t : dump.transactions()) {
            var account = t.accountId() == null ? null : accMap.get(t.accountId());
            if (t.accountId() != null && account == null) {
                account = accountRepository.findById(t.accountId()).orElse(null);
                if (account == null) {
                    if (skipInvalidData)
                        continue;
                    throw new RuntimeException("Account not found: " + t.accountId());
                }
                if (!account.getGroup().getOwner().getId().equals(userId)) {
                    var acl = account.getGroup().getAcls().stream().filter(a -> a.getUser().getId().equals(userId))
                            .findFirst().orElse(null);
                    if (acl == null || acl.isReadonly()) {
                        if (skipInvalidData)
                            continue;
                        throw new RuntimeException("User " + userId + " is not owner of Account " + t.accountId());
                    }
                }
            }
            var recipient = t.recipientId() == null ? null : accMap.get(t.recipientId());
            if (t.recipientId() != null && recipient == null) {
                recipient = accountRepository.findById(t.recipientId()).orElse(null);
                if (recipient == null) {
                    if (skipInvalidData)
                        continue;
                    throw new RuntimeException("Recipient not found: " + t.recipientId());
                }
                if (!recipient.getGroup().getOwner().getId().equals(userId)) {
                    var acl = recipient.getGroup().getAcls().stream().filter(a -> a.getUser().getId().equals(userId))
                            .findFirst().orElse(null);
                    if (acl == null || acl.isReadonly()) {
                        if (skipInvalidData)
                            continue;
                        throw new RuntimeException("User " + userId + " is not owner of Recipient " + t.recipientId());
                    }
                }
            }
            if (account == null && recipient == null) {
                if (skipInvalidData)
                    continue;
                throw new RuntimeException("Account or recipient not found");
            }
            var category = t.categoryId() == null ? null : catMap.get(t.categoryId());
            if (t.categoryId() != null && category == null) {
                category = categoryRepository.findById(t.categoryId()).orElse(null);
                if (category != null) {
                    catMap.put(t.categoryId(), category);
                }
            }
            var transaction = new Transaction(null, userId, t.opdate(), account, t.debit(), recipient, t.credit(),
                    category, t.currency(), t.party(), t.details(), t.created(), t.updated());
            transactionRepository.save(transaction);
        }
        // rules
        for (var r : dump.rules()) {
            var category = catMap.get(r.categoryId());
            if (category == null) {
                throw new RuntimeException("Category not found: " + r.categoryId());
            }
            var rule = new Rule(null, userId, r.conditionType(), r.conditionValue(), category, r.created(),
                    r.updated());
            ruleRepository.save(rule);
        }
    }
}
