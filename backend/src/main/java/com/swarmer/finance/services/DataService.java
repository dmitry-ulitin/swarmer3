package com.swarmer.finance.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.Dump;
import com.swarmer.finance.dto.DumpGroup;
import com.swarmer.finance.dto.DumpRule;
import com.swarmer.finance.dto.DumpCategory;
import com.swarmer.finance.dto.DumpTransaction;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.AccountGroup;
import com.swarmer.finance.models.Acl;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Rule;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.repositories.CategoryRepository;
import com.swarmer.finance.repositories.GroupRepository;
import com.swarmer.finance.repositories.RuleRepository;
import com.swarmer.finance.repositories.TransactionRepository;
import com.swarmer.finance.repositories.UserRepository;
import com.swarmer.finance.repositories.AccountRepository;
import com.swarmer.finance.repositories.AclRepository;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
public class DataService {
    private final AclRepository aclRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;

    public DataService(AclRepository aclRepository, UserRepository userRepository, GroupRepository groupRepository,
            AccountRepository accountRepository, CategoryRepository categoryRepository,
            TransactionRepository transactionRepository, RuleRepository ruleRepository) {
        this.aclRepository = aclRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
    }

    public Dump getDump(Long userId) {
        var categories = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(userId)).stream()
                .sorted((c1, c2) -> (int) (c1.getLevel() - c2.getLevel()))
                .filter(c -> c.getOwnerId() != null).map(DumpCategory::from).toList();
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(userId)).stream().map(DumpGroup::from).toList();
        var transactions = transactionRepository.findAllByOwnerId(userId).map(DumpTransaction::from).toList();
        var rules = ruleRepository.findAllByOwnerId(userId).stream().map(DumpRule::from).toList();
        return new Dump(userId, LocalDateTime.now(), groups, categories, transactions, rules);
    }

    public void loadDump(Long userId, Dump dump) {
        var owner = userRepository.findById(userId).orElseThrow();
        // remove old data
        log.info("Removing old data for user {}", userId);
        transactionRepository.removeByOwnerId(userId);
        ruleRepository.removeByOwnerId(userId);
        categoryRepository.removeByOwnerId(userId);
        groupRepository.deleteByOwnerId(userId);
        // groups
        var accMap = new HashMap<Long, Long>();
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(userId));
        var tryToUpdate = dump.ownerId().equals(userId)
                && dump.groups().stream().noneMatch(d -> groups.stream().noneMatch(g -> d.id().equals(g.getId())));
        if (!tryToUpdate) {
            log.warn("Dump contains data from a different user or a different db");
            // try to delete all groups
            for (var group : groups) {
                var deleted = 0;
                for (var account : group.getAccounts()) {
                    if (!transactionRepository.existsByAccountIdOrRecipientId(account.getId(), account.getId())) {
                        accountRepository.delete(account);
                        deleted++;
                    } else {
                        log.warn("Account {} has transactions, group {} has not been deleted", account.getId(),
                                group.getId());
                    }
                }
                if (deleted == group.getAccounts().size()) {
                    groupRepository.delete(group);
                }
            }            
            groups.clear();
        }
        if (tryToUpdate) {
            // update existing groups
            var updated = 0;
            for (var group : groups) {
                var existingGroup = dump.groups().stream().filter(d -> d.id().equals(group.getId())).findFirst().orElse(null);
                if (existingGroup == null) {
                    group.setDeleted(true);
                    groupRepository.save(group);
                } else {
                    group.setName(existingGroup.name());
                    group.setDeleted(existingGroup.deleted());
                    group.setUpdated(existingGroup.updated());
                    for (var account : group.getAccounts()) {
                        var updatedAccount = existingGroup.accounts().stream().filter(a -> a.id().equals(account.getId()))
                                .findFirst().orElse(null);
                        if (updatedAccount == null) {
                            account.setDeleted(true);
                        } else {
                            account.setName(updatedAccount.name());
                            account.setCurrency(updatedAccount.currency());
                            account.setStart_balance(updatedAccount.start_balance());
                            account.setDeleted(updatedAccount.deleted());
                            account.setUpdated(updatedAccount.updated());
                        }
                    }
                    // save permissions
                    for (var updatedAcl : existingGroup.acls()) {
                        var acl = group.getAcls().stream().filter(a -> a.getUserId().equals(updatedAcl.userId()))
                                .findFirst()
                                .orElse(null);
                        if (acl == null) {
                            var user = userRepository.findById(updatedAcl.userId()).orElse(null);
                            if (user == null) {
                                log.warn("User {} not found, acl for group {} has been dropped", updatedAcl.userId(),
                                        group.getId());
                                continue;
                            }
                            group.getAcls()
                                    .add(new Acl(group.getId(), group, updatedAcl.userId(), user,
                                            updatedAcl.admin(),
                                            updatedAcl.readonly(), updatedAcl.name(),
                                            updatedAcl.deleted(), updatedAcl.created(), updatedAcl.updated()));
                        } else {
                            acl.setAdmin(updatedAcl.admin());
                            acl.setReadonly(updatedAcl.readonly());
                            acl.setUpdated(updatedAcl.updated());
                        }
                    }
                    group.getAcls().stream().filter(
                            a -> existingGroup.acls().stream().noneMatch(p -> p.userId().equals(a.getUserId())))
                            .forEach(a -> aclRepository.delete(a));
                    group.setAcls(group.getAcls().stream().filter(
                            a -> existingGroup.acls().stream().anyMatch(p -> p.userId().equals(a.getUserId())))
                            .collect(Collectors.toList()));
                    groupRepository.save(group);
                    updated++;
                }
            }
            log.info("Updated {} groups", updated);
        }
        // insert new groups
        var inserted = 0;
        for (var g : dump.groups()) {
            var existingGroup = groups.stream().filter(group -> group.getId().equals(g.id())).findFirst().orElse(null);
            if (existingGroup == null) {
                var group = new AccountGroup();
                group.setOwner(owner);
                group.setName(g.name());
                group.setDeleted(g.deleted());
                group.setCreated(g.created());
                group.setUpdated(g.updated());
                group.setAccounts(new ArrayList<>());
                group.setAcls(new ArrayList<>());
                groupRepository.save(group);
                for (var a : g.accounts()) {
                    var account = new Account(null, group, a.name(), a.currency(), a.start_balance(), a.deleted(),
                            a.created(), a.updated());
                    accountRepository.save(account);
                    accMap.put(a.id(), account.getId());
                    group.getAccounts().add(account);
                }
                for (var a : g.acls()) {
                    var user = userRepository.findById(a.userId()).orElse(null);
                    if (user == null) {
                        log.warn("User {} not found, acl for group {}({}) has been dropped", a.userId(), group.getId(),
                                g.id());
                        continue;
                    }
                    var acl = new Acl(group.getId(), group, a.userId(), user, a.admin(), a.readonly(),
                            a.name(),
                            a.deleted(), a.created(), a.updated());
                    aclRepository.save(acl);
                    group.getAcls().add(acl);
                }
                groupRepository.save(group);
                inserted++;
            }
        }
        log.info("Inserted {} groups", inserted);
        // categories
        var catMap = new HashMap<Long, Long>();
        for (var c : dump.categories()) {
            var parentId = catMap.getOrDefault(c.parentId(), c.parentId());
            var parent = categoryRepository.findById(parentId).orElse(null);
            var category = new Category(null, userId, parentId, parent, c.name(), c.created(), c.updated());
            categoryRepository.save(category);
            catMap.put(c.id(), category.getId());
        }
        log.info("Inserted {} categories", dump.categories().size());
        // transactions
        inserted = 0;
        for (var t : dump.transactions()) {
            var account = t.accountId() == null ? null
                    : accountRepository.findById(accMap.getOrDefault(t.accountId(), t.accountId())).orElse(null);
            var recipient = t.recipientId() == null ? null
                    : accountRepository.findById(accMap.getOrDefault(t.recipientId(), t.recipientId()))
                            .orElse(null);
            var category = t.categoryId() == null ? null
                    : categoryRepository.findById(catMap.getOrDefault(t.categoryId(), t.categoryId())).orElse(null);
            if (t.accountId() != null && account == null || t.recipientId() != null && recipient == null) {
                log.warn("Invalid account or recipient, transaction {} has been dropped", t.id());
                continue;
            }
            // TODO - check if account group has same owner
            transactionRepository.save(new Transaction(null, owner, t.opdate(), account, t.debit(),
                    recipient, t.credit(), category, t.currency(), t.party(), t.details(), t.created(),
                    t.updated()));
            inserted++;
        }
        log.info("Inserted {} of {} transactions", inserted, dump.transactions().size());
        // rules
        for (var r : dump.rules()) {
            var category = r.categoryId() == null ? null
                    : categoryRepository.findById(catMap.getOrDefault(r.categoryId(), r.categoryId())).orElse(null);
            ruleRepository.save(
                    new Rule(null, userId, r.conditionType(), r.conditionValue(), category, r.created(), r.updated()));
        }
        log.info("Inserted {} rules", dump.rules().size());
    }
}
