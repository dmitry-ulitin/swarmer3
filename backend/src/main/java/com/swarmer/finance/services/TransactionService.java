package com.swarmer.finance.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.CategoryDto;
import com.swarmer.finance.dto.CategorySum;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.Summary;
import com.swarmer.finance.dto.TransactionDto;
import com.swarmer.finance.dto.TransactionSum;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.repositories.TransactionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AclService aclService;
    private final CategoryService categoryService;
    private final EntityManager entityManager;

    public TransactionService(TransactionRepository transactionRepository, AclService aclService,
            CategoryService categoryService, EntityManager entityManager) {
        this.transactionRepository = transactionRepository;
        this.aclService = aclService;
        this.categoryService = categoryService;
        this.entityManager = entityManager;
    }

    public List<TransactionSum> getBalances(Collection<Long> accList, LocalDateTime from, LocalDateTime to, Long id) {
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(TransactionSum.class);
        var root = criteriaQuery.from(Transaction.class);
        var where = builder.or(root.get("account").get("id").in(accList), root.get("recipient").get("id").in(accList));
        if (from != null) {
            var greaterThanOrEqualTo = builder.greaterThanOrEqualTo(root.<LocalDateTime>get("opdate"), from);
            where = builder.and(where, greaterThanOrEqualTo);
        }
        if (to != null) {
            var lessThanOpdate = builder.lessThan(root.<LocalDateTime>get("opdate"), to);
            if (id != null) {
                lessThanOpdate = builder.or(lessThanOpdate,
                        builder.and(builder.equal(root.<LocalDateTime>get("opdate"), to),
                                builder.lessThan(root.get("id"), id)));
            }
            where = builder.and(where, lessThanOpdate);
        }
        criteriaQuery.multiselect(root.get("account").get("id"), root.get("recipient").get("id"),
                builder.sum(root.get("debit")).alias("debit"),
                builder.sum(root.get("credit")).alias("credit"),
                builder.max(root.get("opdate")).alias("opdate"))
                .where(where)
                .groupBy(root.get("account").get("id"), root.get("recipient").get("id"));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public TransactionDto getTransaction(Long id, Long userId) {
        var transaction = transactionRepository.findById(id).orElseThrow();
        var accList = new ArrayList<Long>();
        if (transaction.getAccount() != null) {
            accList.add(transaction.getAccount().getId());
        }
        if (transaction.getRecipient() != null) {
            accList.add(transaction.getRecipient().getId());
        }
        var balances = getBalances(accList, null, transaction.getOpdate(), transaction.getId());
        BigDecimal accountBalance = transaction.getAccount() == null ? null
                : transaction.getAccount().getStartBalance()
                        .add(balances.stream().filter(b -> transaction.getAccount().getId().equals(b.accountId()))
                                .map(b -> b.debit()).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal recipientBalance = transaction.getRecipient() == null ? null
                : transaction.getRecipient().getStartBalance()
                        .add(balances.stream().filter(b -> transaction.getRecipient().getId().equals(b.accountId()))
                                .map(b -> b.credit()).reduce(BigDecimal.ZERO, BigDecimal::add));
        return TransactionDto.fromEntity(transaction, userId, accountBalance, recipientBalance);
    }

    public List<TransactionDto> getTransactions(Long userId, Collection<Long> accountIdsFilter, String search,
            Long categoryId, String currency, LocalDateTime from, LocalDateTime to, int offset, int limit) {
        var userAccounts = aclService.getAccounts(userId);
        var validAccountIds = userAccounts.stream()
                .filter(a -> currency == null || currency.isBlank() || currency.equals(a.getCurrency()))
                .map(Account::getId)
                .filter(id -> accountIdsFilter == null || accountIdsFilter.isEmpty() || accountIdsFilter.contains(id))
                .toList();
        if (validAccountIds.isEmpty()) {
            return List.of();
        }
        var trx = queryTransactions(userId, validAccountIds, search, categoryId, from, to, offset, limit);
        if (trx.isEmpty()) {
            return List.of();
        }
        var actualAccountIds = trx.stream().flatMap(t -> {
            var list = new ArrayList<Long>();
            if (t.getAccount() != null) {
                list.add(t.getAccount().getId());
            }
            if (t.getRecipient() != null) {
                list.add(t.getRecipient().getId());
            }
            return list.stream();
        }).toList();
        var last = trx.getLast();
        var calcBalances = (search == null || search.isBlank()) && categoryId == null;
        List<TransactionSum> rawBalnces = calcBalances
                ? getBalances(actualAccountIds, null, last.getOpdate(), last.getId())
                : List.of();
        Map<Long, BigDecimal> accBalances = new HashMap<>();
        var dto = new TransactionDto[trx.size()];
        // iterate over transactions in reverse order
        for (var i = trx.size() - 1; i >= 0; i--) {
            var t = trx.get(i);
            var account = t.getAccount();
            var recipient = t.getRecipient();
            BigDecimal accountBalance = null;
            BigDecimal recipientBalance = null;
            if (calcBalances) {
                if (account != null) {
                    accountBalance = accBalances.computeIfAbsent(account.getId(), id -> account.getStartBalance()
                            .subtract(rawBalnces.stream().filter(b -> account.getId().equals(b.accountId()))
                                    .map(b -> b.debit()).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .add(rawBalnces.stream().filter(b -> account.getId().equals(b.recipientId()))
                                    .map(b -> b.credit()).reduce(BigDecimal.ZERO, BigDecimal::add)));
                    accountBalance = accountBalance.subtract(t.getDebit());
                    accBalances.put(account.getId(), accountBalance);
                }
                if (recipient != null) {
                    recipientBalance = accBalances.computeIfAbsent(recipient.getId(), id -> recipient.getStartBalance()
                            .subtract(rawBalnces.stream().filter(b -> recipient.getId().equals(b.accountId()))
                                    .map(b -> b.debit()).reduce(BigDecimal.ZERO, BigDecimal::add))
                            .add(rawBalnces.stream().filter(b -> recipient.getId().equals(b.recipientId()))
                                    .map(b -> b.credit()).reduce(BigDecimal.ZERO, BigDecimal::add)));
                    recipientBalance = recipientBalance.add(t.getCredit());
                    accBalances.put(recipient.getId(), recipientBalance);
                }
            }
            dto[i] = TransactionDto.fromEntity(t, userId, accountBalance, recipientBalance);
        }
        return Arrays.asList(dto);
    }

    public TransactionDto createTransaction(TransactionDto dto, Long userId) {
        var transaction = new Transaction();
        return saveTransaction(transaction, dto, userId);
    }

    public TransactionDto updateTransaction(TransactionDto dto, Long userId) {
        var transaction = transactionRepository.findById(dto.id()).orElseThrow();
        return saveTransaction(transaction, dto, userId);
    }

    private TransactionDto saveTransaction(Transaction trx, TransactionDto dto, Long userId) {
        if (trx.getId() != null) {
            updateCorrections(userId, trx.getAccount(), trx.getDebit().negate(), trx.getOpdate(), trx.getId(), false);
            updateCorrections(userId, trx.getRecipient(), trx.getCredit(), trx.getOpdate(), trx.getId(), false);
        }
        trx.setOwnerId(userId);
        trx.setOpdate(dto.opdate());
        trx.setAccount(dto.account() == null ? null : entityManager.find(Account.class, dto.account().id()));
        trx.setDebit(dto.debit());
        trx.setRecipient(dto.recipient() == null ? null : entityManager.find(Account.class, dto.recipient().id()));
        trx.setCredit(dto.credit());
        trx.setCategory(dto.category() == null ? null : categoryService.getCategory(dto.category(), userId));
        trx.setCurrency(dto.currency());
        trx.setParty(dto.party());
        trx.setDetails(dto.details());
        trx.setUpdated(LocalDateTime.now());
        transactionRepository.save(trx);
        updateCorrections(userId, trx.getAccount(), dto.debit(), dto.opdate(), trx.getId(), true);
        updateCorrections(userId, trx.getRecipient(), dto.credit().negate(), dto.opdate(), trx.getId(), true);
        return getTransaction(trx.getId(), userId);
    }

    public void deleteTransaction(Long id, Long userId) {
        var trx = transactionRepository.findById(id).orElseThrow();
        updateCorrections(userId, trx.getAccount(), trx.getDebit().negate(), trx.getOpdate(), trx.getId(), true);
        updateCorrections(userId, trx.getRecipient(), trx.getCredit(), trx.getOpdate(), trx.getId(), true);
        transactionRepository.delete(trx);
    }

    public void saveImport(Long userId, Long accountId, List<ImportDto> records) {
        var account = entityManager.find(Account.class, accountId);
        var minOpdate = records.stream().map(ImportDto::getOpdate).min(LocalDateTime::compareTo).orElse(null);
        var corrections = queryTransactions(userId, List.of(accountId), null,
                (long) (TransactionType.CORRECTION.getValue()), minOpdate, null, 0, 0);
        for (var record : records) {
            if (record.isSelected()) {
                var transaction = new Transaction();
                transaction.setOwnerId(userId);
                transaction.setOpdate(record.getOpdate());
                if (record.getType() == TransactionType.EXPENSE) {
                    transaction.setAccount(account);
                } else {
                    transaction.setRecipient(account);
                }
                transaction.setDebit(record.getDebit());
                transaction.setCredit(record.getCredit());
                transaction.setCategory(categoryService.getCategory(record.getCategory(), userId));
                transaction.setCurrency(record.getCurrency());
                transaction.setParty(record.getParty());
                transaction.setDetails(record.getDetails());
                transactionRepository.save(transaction);
                corrections.stream().filter(c -> c.getOpdate().isAfter(record.getOpdate()))
                        .forEach(c -> updateCorrection(c, record.getType() == TransactionType.EXPENSE
                                ? record.getDebit()
                                : record.getCredit().negate()));
            } else if (record.getId() != null) {
                var update = false;
                var transaction = transactionRepository.findById(record.getId()).orElseThrow();
                if (transaction.getParty() == null || transaction.getParty().isBlank()) {
                    transaction.setParty(record.getParty());
                    update = true;
                }
                if (transaction.getDetails() == null || transaction.getDetails().isBlank()) {
                    transaction.setDetails(record.getDetails());
                    update = true;
                }
                if (update) {
                    transaction.setOwnerId(userId);
                    transaction.setUpdated(LocalDateTime.now());
                    transactionRepository.save(transaction);
                }
            }
        }
        corrections.stream().filter(c -> c.getCredit().equals(BigDecimal.ZERO) && c.getDebit().equals(BigDecimal.ZERO))
                .forEach(c -> transactionRepository.delete(c));
        ;
    }

    public Collection<Summary> getSummary(Long userId, Collection<Long> accountIdsFilter, LocalDateTime from,
            LocalDateTime to) {
        var userAccounts = aclService.getAccounts(userId);
        var validAccounts = userAccounts.stream()
                .filter(a -> accountIdsFilter == null || accountIdsFilter.isEmpty()
                        || accountIdsFilter.contains(a.getId()))
                .collect(Collectors.toMap(a -> a.getId(), a -> a));
        if (validAccounts.isEmpty()) {
            return List.of();
        }
        var result = validAccounts.values().stream().map(Account::getCurrency).distinct()
                .collect(Collectors.toMap(c -> c,
                        c -> new Summary(c, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        getBalances(validAccounts.keySet(), from, to, null).forEach(b -> {
            if (validAccounts.containsKey(b.accountId())) {
                var account = validAccounts.get(b.accountId());
                var summary = result.get(account.getCurrency());
                if (b.recipientId() == null) {
                    // expense
                    summary.setDebit(summary.getDebit().add(b.debit()));
                } else {
                    // transfer
                    if (!validAccounts.containsKey(b.recipientId())) {
                        summary.setTransfers_debit(summary.getTransfers_debit().add(b.debit()));
                    }
                }
            }
            if (validAccounts.containsKey(b.recipientId())) {
                var recipient = validAccounts.get(b.recipientId());
                var summary = result.get(recipient.getCurrency());
                if (b.accountId() == null) {
                    // income
                    summary.setCredit(summary.getCredit().add(b.credit()));
                } else {
                    // transfer
                    if (!validAccounts.containsKey(b.accountId())) {
                        summary.setTransfers_credit(summary.getTransfers_credit().add(b.credit()));
                    }
                }
            }
        });
        return result.values();
    }

    public Collection<CategorySum> getCategoriesSummary(Long userId, TransactionType type,
            Collection<Long> accountIdsFilter,
            LocalDateTime from, LocalDateTime to) {
        if (type != TransactionType.EXPENSE && type != TransactionType.INCOME) {
            return List.of();
        }
        var userAccounts = aclService.getAccounts(userId);
        var validAccounts = userAccounts.stream().map(Account::getId)
                .filter(a -> accountIdsFilter == null || accountIdsFilter.isEmpty()
                        || accountIdsFilter.contains(a))
                .toList();
        if (validAccounts.isEmpty()) {
            return List.of();
        }
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(CategorySum.class);
        var root = criteriaQuery.from(Transaction.class);
        var category = root.join("category", JoinType.LEFT);
        var where = type == TransactionType.EXPENSE
                ? builder.and(root.get("account").get("id").in(validAccounts), root.get("recipient").isNull())
                : builder.and(root.get("account").isNull(), root.get("recipient").get("id").in(validAccounts));
        if (from != null) {
            var greaterThanOrEqualTo = builder.greaterThanOrEqualTo(root.<LocalDateTime>get("opdate"), from);
            where = builder.and(where, greaterThanOrEqualTo);
        }
        if (to != null) {
            var lessThanOpdate = builder.lessThan(root.<LocalDateTime>get("opdate"), to);
            where = builder.and(where, lessThanOpdate);
        }
        if (type == TransactionType.EXPENSE) {
            criteriaQuery.multiselect(category, root.get("account").get("currency"),
                    builder.sum(root.get("debit")).alias("sum")).where(where)
                    .groupBy(category, root.get("account").get("currency"));
        } else {
            criteriaQuery.multiselect(category, root.get("recipient").get("currency"),
                    builder.sum(root.get("credit")).alias("sum")).where(where)
                    .groupBy(category, root.get("recipient").get("currency"));
        }
        var categorySums = entityManager.createQuery(criteriaQuery).getResultList();
        var typecat = CategoryDto.fromEntity(entityManager.find(Category.class, type.getValue()));
        categorySums.forEach(cs -> {
            if (cs.getCategory() == null) {
                cs.setCategory(typecat);
            } else if (cs.getCategory().ownerId() != userId && cs.getCategory().level() > 0
                    || cs.getCategory().level() > 1) {
                Category cat = categoryService.getCategory(cs.getCategory(), userId);
                while (cat.getParent().getParent() != null) {
                    cat = cat.getParent();
                }
                cs.setCategory(CategoryDto.fromEntity(cat));
            }
        });
        categorySums = categorySums.stream()
                .collect(Collectors.groupingBy(cs -> Pair.of(cs.getCurrency(), cs.getCategory()))).entrySet().stream()
                .map(e -> e.getValue().stream().reduce(
                        new CategorySum(e.getKey().getSecond(), e.getKey().getFirst(), BigDecimal.ZERO), (a, g) -> {
                            a.setSum((a.getSum().add(g.getSum())));
                            return a;
                        }))
                .sorted((a, b) -> a.getCategory().fullName().compareToIgnoreCase(b.getCategory().fullName())).toList();
        return categorySums;

    }

    private void updateCorrections(Long userId, Account account, BigDecimal amount, LocalDateTime opdate, Long id,
            Boolean removeZeros) {
        if (account == null) {
            return;
        }
        var corrections = queryTransactions(userId, List.of(account.getId()), null,
                (long) (TransactionType.CORRECTION.getValue()), opdate, null, 0, 0);
        for (var correction : corrections) {
            updateCorrection(correction, amount);
            if (removeZeros && correction.getCredit().equals(BigDecimal.ZERO)
                    && correction.getDebit().equals(BigDecimal.ZERO)) {
                transactionRepository.delete(correction);
            }
        }
    }

    private void updateCorrection(Transaction correction, BigDecimal amount) {
        if (correction.getAccount() == null) {
            amount = amount.negate();
        }
        correction.setCredit(correction.getCredit().subtract(amount));
        correction.setDebit(correction.getCredit());
        if (correction.getCredit().compareTo(BigDecimal.ZERO) < 0) {
            correction.setCredit(correction.getCredit().negate());
            correction.setDebit(correction.getCredit());
            var account = correction.getAccount();
            correction.setAccount(correction.getRecipient());
            correction.setRecipient(account);
        }
    }

    public List<Transaction> queryTransactions(Long userId, Collection<Long> ai,
            String search, Long categoryId, LocalDateTime from, LocalDateTime to, int offset, int limit) {
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(Transaction.class);
        var root = criteriaQuery.from(Transaction.class);
        var where = builder.or(root.get("account").get("id").in(ai), root.get("recipient").get("id").in(ai));
        if (search != null && !search.isBlank()) {
            var pattern = "%" + search.toUpperCase() + "%";
            var details = builder.like(builder.upper(root.get("details")), pattern);
            var party = builder.like(builder.upper(root.get("party")), pattern);
            var category = builder.like(builder.upper(root.join("category", JoinType.LEFT).get("name")), pattern);
            where = builder.and(where, builder.or(category, details, party));
        }
        if (categoryId != null) {
            if (categoryId == -TransactionType.EXPENSE.getValue()) {
                where = builder.and(where, root.get("category").isNull(), root.get("recipient").isNull());
            } else if (categoryId == -TransactionType.INCOME.getValue()) {
                where = builder.and(where, root.get("category").isNull(), root.get("account").isNull());
            } else if (categoryId == TransactionType.EXPENSE.getValue()) {
                where = builder.and(where, root.get("recipient").isNull());
            } else if (categoryId == TransactionType.INCOME.getValue()) {
                where = builder.and(where, root.get("account").isNull());
            } else if (categoryId == TransactionType.TRANSFER.getValue()) {
                where = builder.and(where, root.get("account").isNotNull(), root.get("recipient").isNotNull());
            } else if (categoryId == TransactionType.CORRECTION.getValue()) {
                where = builder.and(where, root.get("category").get("id").in(categoryId));
            } else {
                var categories = categoryService.getCategoriesFilter(userId, categoryId);
                where = builder.and(where, root.get("category").get("id").in(categories));
            }
        }
        if (from != null) {
            where = builder.and(where, builder.greaterThanOrEqualTo(root.get("opdate"), from));
        }
        if (to != null) {
            where = builder.and(where, builder.lessThan(root.get("opdate"), to));
        }
        criteriaQuery = criteriaQuery.where(where).orderBy(builder.desc(root.get("opdate")),
                builder.desc(root.get("id")));
        var typedQuery = entityManager.createQuery(criteriaQuery);
        if (offset > 0) {
            typedQuery = typedQuery.setFirstResult(offset);
        }
        if (limit > 0) {
            typedQuery = typedQuery.setMaxResults(limit);
        }
        var trx = typedQuery.getResultList();
        return trx;
    }
}
