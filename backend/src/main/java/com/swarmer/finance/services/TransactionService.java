package com.swarmer.finance.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.CategoryIdSum;
import com.swarmer.finance.dto.CategorySum;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.Summary;
import com.swarmer.finance.dto.TransactionDto;
import com.swarmer.finance.dto.TransactionSum;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.repositories.AccountRepository;
import com.swarmer.finance.repositories.TransactionRepository;
import com.swarmer.finance.repositories.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AclService aclService;
    private final CategoryService categoryService;
    private final EntityManager entityManager;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
            UserRepository userRepository, AclService aclService,
            CategoryService categoryService, EntityManager entityManager) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.aclService = aclService;
        this.categoryService = categoryService;
        this.entityManager = entityManager;
    }

    public List<TransactionDto> getTransactions(Long userId, Collection<Long> accountIds,
            String search, Long categoryId, String currency, LocalDateTime from, LocalDateTime to, int offset, int limit) {
        var ai = accountIds.isEmpty() ? aclService.findAccounts(userId).map(a -> a.getId()).toList()
                : new ArrayList<>(accountIds);
        if (currency != null && !currency.isBlank()) {
            ai = accountRepository.findByIdIn(ai).stream().filter(a -> currency.equals(a.getCurrency()))
                    .map(a -> a.getId()).toList();
        }
        var trx = queryTransactions(userId, ai, search, categoryId, from, to, offset, limit);
        ai = LongStream.concat(trx.stream().filter(t -> t.getAccount() != null).mapToLong(t -> t.getAccount().getId()),
                trx.stream().filter(t -> t.getRecipient() != null).mapToLong(t -> t.getRecipient().getId())).distinct()
                .boxed().toList();
        if (trx.isEmpty()) {
            return List.of();
        }
        var calcBalances = (search == null || search.isBlank()) && categoryId == null;
        List<TransactionSum> rawBalnces = calcBalances ? getBalances(ai, null, trx.get(trx.size() - 1).getOpdate(),
                trx.get(trx.size() - 1).getId()) : List.of();
        Map<Long, Double> accBalances = new HashMap<>();
        var dto = new TransactionDto[trx.size()];
        for (int index = trx.size() - 1; index >= 0; index--) {
            var transaction = trx.get(index);
            Double accountBalance = null;
            Double recipientBalance = null;
            if (transaction.getAccount() != null && calcBalances) {
                accountBalance = accBalances.get(transaction.getAccount().getId());
                if (accountBalance == null) {
                    accountBalance = transaction.getAccount().getStartBalance();
                    accountBalance -= rawBalnces.stream()
                            .filter(b -> transaction.getAccount().getId().equals(b.getAccountId()))
                            .mapToDouble(b -> b.getDebit()).sum();
                    accountBalance += rawBalnces.stream()
                            .filter(b -> transaction.getAccount().getId().equals(b.getRecipientId()))
                            .mapToDouble(b -> b.getCredit()).sum();
                }
                accountBalance -= transaction.getDebit();
                accBalances.put(transaction.getAccount().getId(), accountBalance);
            }
            if (transaction.getRecipient() != null && calcBalances) {
                recipientBalance = accBalances.get(transaction.getRecipient().getId());
                if (recipientBalance == null) {
                    recipientBalance = transaction.getRecipient().getStartBalance();
                    recipientBalance -= rawBalnces.stream()
                            .filter(b -> transaction.getRecipient().getId().equals(b.getAccountId()))
                            .mapToDouble(b -> b.getDebit()).sum();
                    recipientBalance += rawBalnces.stream()
                            .filter(b -> transaction.getRecipient().getId().equals(b.getRecipientId()))
                            .mapToDouble(b -> b.getCredit()).sum();
                }
                recipientBalance += transaction.getCredit();
                accBalances.put(transaction.getRecipient().getId(), recipientBalance);
            }
            dto[index] = TransactionDto.from(transaction, userId, accountBalance, recipientBalance);
        }
        return Arrays.asList(dto);
    }

    public TransactionDto getTransaction(Long id, Long userId) {
        var transaction = transactionRepository.findById(id).orElseThrow();
        Double accountBalance = null;
        Double recipientBalance = null;
        var ai = new ArrayList<Long>();
        if (transaction.getAccount() != null) {
            ai.add(transaction.getAccount().getId());
        }
        if (transaction.getRecipient() != null) {
            ai.add(transaction.getRecipient().getId());
        }
        var balances = getBalances(ai, null, transaction.getOpdate(), transaction.getId());
        if (transaction.getAccount() != null) {
            accountBalance = transaction.getAccount().getStartBalance();
            accountBalance -= balances.stream().filter(b -> transaction.getAccount().getId().equals(b.getAccountId()))
                    .mapToDouble(b -> b.getDebit()).sum();
            accountBalance += balances.stream().filter(b -> transaction.getAccount().getId().equals(b.getRecipientId()))
                    .mapToDouble(b -> b.getCredit()).sum();
            accountBalance -= transaction.getDebit();
        }
        if (transaction.getRecipient() != null) {
            recipientBalance = transaction.getRecipient().getStartBalance();
            recipientBalance -= balances.stream()
                    .filter(b -> transaction.getRecipient().getId().equals(b.getAccountId()))
                    .mapToDouble(b -> b.getDebit()).sum();
            recipientBalance += balances.stream()
                    .filter(b -> transaction.getRecipient().getId().equals(b.getRecipientId()))
                    .mapToDouble(b -> b.getCredit()).sum();
            recipientBalance += transaction.getCredit();
        }
        return TransactionDto.from(transaction, userId, accountBalance, recipientBalance);
    }

    public TransactionDto createTransaction(TransactionDto dto, Long userId) {
        var entity = new Transaction();
        entity.setOwner(userRepository.findById(userId).orElseThrow());
        entity.setCreated(LocalDateTime.now());
        if (dto.category() != null) {
            var category = categoryService.getCategory(dto.category(), entity.getOwner().getId());
            entity.setCategory(category);
        }
        dto2entity(dto, entity);
        transactionRepository.save(entity);
        if (entity.getAccount() != null) {
            updateCorrections(entity.getAccount().getId(), entity.getDebit(), entity.getOpdate(), null, true);
        }
        if (entity.getRecipient() != null) {
            updateCorrections(entity.getRecipient().getId(), -entity.getCredit(), entity.getOpdate(), null, true);
        }
        return getTransaction(entity.getId(), userId);
    }

    public TransactionDto updateTransaction(TransactionDto dto, Long userId) {
        var entity = transactionRepository.findById(dto.id()).orElseThrow();
        if (entity.getAccount() != null) {
            updateCorrections(entity.getAccount().getId(), -entity.getDebit(), entity.getOpdate(), entity.getId(),
                    false);
        }
        if (entity.getRecipient() != null) {
            updateCorrections(entity.getRecipient().getId(), entity.getCredit(), entity.getOpdate(), entity.getId(),
                    false);
        }
        entity.setOwner(userRepository.findById(userId).orElseThrow());
        var category = dto.category() == null ? null : categoryService.getCategory(dto.category(), entity.getOwner().getId());
        entity.setCategory(category);
        dto2entity(dto, entity);
        transactionRepository.save(entity);
        if (entity.getAccount() != null) {
            updateCorrections(entity.getAccount().getId(), entity.getDebit(), entity.getOpdate(), entity.getId(), true);
        }
        if (entity.getRecipient() != null) {
            updateCorrections(entity.getRecipient().getId(), -entity.getCredit(), entity.getOpdate(), entity.getId(),
                    true);
        }
        return getTransaction(entity.getId(), userId);
    }

    public void deleteTransaction(Long id, Long userId) {
        var entity = transactionRepository.findById(id).orElseThrow();
        if (entity.getAccount() != null) {
            updateCorrections(entity.getAccount().getId(), -entity.getDebit(), entity.getOpdate(), entity.getId(),
                    true);
        }
        if (entity.getRecipient() != null) {
            updateCorrections(entity.getRecipient().getId(), entity.getCredit(), entity.getOpdate(), entity.getId(),
                    true);
        }
        transactionRepository.deleteById(id);
    }

    public List<TransactionSum> getBalances(Collection<Long> ai) {
        return getBalances(ai, null, null, null);
    }

    // select t.account.id, t.recipient.id, sum(t.debit), sum(t.credit) from
    // transactions t where t.account.id in :a or t.recipient.id in :a group by
    // t.account.id, t.recipient.id
    public List<TransactionSum> getBalances(Collection<Long> ai, LocalDateTime from, LocalDateTime to, Long id) {
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(TransactionSum.class);
        var root = criteriaQuery.from(Transaction.class);
        var where = builder.or(root.get("account").get("id").in(ai), root.get("recipient").get("id").in(ai));
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
                builder.sumAsDouble(root.get("debit")).alias("debit"),
                builder.sumAsDouble(root.get("credit")).alias("credit"),
                builder.max(root.get("opdate")).alias("opdate"))
                .where(where)
                .groupBy(root.get("account").get("id"), root.get("recipient").get("id"));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public Collection<Summary> getSummary(Long userId, Collection<Long> accountIds, LocalDateTime from,
            LocalDateTime to) {
        Map<Long, Account> userAccounts = aclService.findAccounts(userId)
                .collect(Collectors.toMap(a -> a.getId(), a -> a));
        Map<Long, Account> resultAccounts = (accountIds.isEmpty() ? userAccounts.values()
                : accountRepository.findByIdIn(accountIds))
                .stream().collect(Collectors.toMap(a -> a.getId(), a -> a));
        var result = resultAccounts.values().stream().map(a -> a.getCurrency())
                .distinct()
                .collect(Collectors.toMap(c -> c, c -> new Summary(c, .0, .0, .0, .0)));
        var balances = getBalances(resultAccounts.keySet(), from, to, null);
        for (var b : balances) {
            if (resultAccounts.containsKey(b.getAccountId())) {
                var aCurrency = resultAccounts.get(b.getAccountId()).getCurrency();
                if (userAccounts.containsKey(b.getRecipientId())) {
                    if (!resultAccounts.containsKey(b.getRecipientId())) {
                        var rCurrency = userAccounts.get(b.getRecipientId()).getCurrency();
                        if (result.containsKey(rCurrency)) {
                            var rSummary = result.get(rCurrency);
                            rSummary.setTransfers_debit(rSummary.getTransfers_debit() + b.getCredit());
                        } else {
                            var rSummary = result.get(aCurrency);
                            rSummary.setTransfers_debit(rSummary.getTransfers_debit() + b.getDebit());
                        }
                    }
                } else {
                    var aSummary = result.get(aCurrency);
                    aSummary.setDebit(aSummary.getDebit() + b.getDebit());
                }
            }
            if (resultAccounts.containsKey(b.getRecipientId())) {
                var rCurrency = resultAccounts.get(b.getRecipientId()).getCurrency();
                if (userAccounts.containsKey(b.getAccountId())) {
                    if (!resultAccounts.containsKey(b.getAccountId())) {
                        var aCurrency = userAccounts.get(b.getAccountId()).getCurrency();
                        if (result.containsKey(aCurrency)) {
                            var aSummary = result.get(aCurrency);
                            aSummary.setTransfers_credit(aSummary.getTransfers_credit() + b.getDebit());
                        } else {
                            var aSummary = result.get(rCurrency);
                            aSummary.setTransfers_credit(aSummary.getTransfers_credit() + b.getCredit());
                        }
                    }
                } else {
                    var rSummary = result.get(rCurrency);
                    rSummary.setCredit(rSummary.getCredit() + b.getCredit());
                }
            }
        }
        return result.values();
    }

    public Collection<CategorySum> getCategoriesSummary(Long userId, TransactionType type, Collection<Long> accountIds,
            LocalDateTime from,
            LocalDateTime to) {
        var ai = accountIds.isEmpty() ? aclService.findAccounts(userId).map(a -> a.getId()).toList() : accountIds;
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(CategoryIdSum.class);
        var root = criteriaQuery.from(Transaction.class);
        var where = type == TransactionType.EXPENSE
                ? builder.and(root.get("account").get("id").in(ai), root.get("recipient").isNull())
                : builder.and(root.get("account").isNull(), root.get("recipient").get("id").in(ai));
        if (from != null) {
            var greaterThanOrEqualTo = builder.greaterThanOrEqualTo(root.<LocalDateTime>get("opdate"), from);
            where = builder.and(where, greaterThanOrEqualTo);
        }
        if (to != null) {
            var lessThanOpdate = builder.lessThan(root.<LocalDateTime>get("opdate"), to);
            where = builder.and(where, lessThanOpdate);
        }
        if (type == TransactionType.EXPENSE) {
            criteriaQuery.multiselect(root.get("category").get("id"),
                    root.get("account").get("currency"),
                    builder.sumAsDouble(root.get("debit")).alias("amount"))
                    .where(where)
                    .groupBy(root.get("category"), root.get("account").get("currency"));
        } else {
            criteriaQuery.multiselect(root.get("category").get("id"),
                    root.get("recipient").get("currency"),
                    builder.sumAsDouble(root.get("credit")).alias("amount"))
                    .where(where)
                    .groupBy(root.get("category").get("id"), root.get("recipient").get("currency"));
        }
        var categoryIdSums = entityManager.createQuery(criteriaQuery).getResultList();
        var categorySums = categoryIdSums.stream()
                .map(r -> new CategorySum(r.getId() == null ? null : entityManager.find(Category.class, r.getId()),
                        r.getCurrency(), r.getSum()))
                .toList();
        // group by to parent category
        for (var cs : categorySums) {
            var category = cs.getCategory();
            if (category != null) {
                while (category.getLevel() > 1) {
                    category = entityManager.find(Category.class, category.getParentId());
                }
                var fullName = category.getFullName();
                category = categorySums.stream().filter(c -> c.getCategory() != null && c.getCategory().getFullName().equals(fullName))
                        .findFirst().map(c -> c.getCategory()).orElse(category);
                cs.setCategory(category);
            } else {
                cs.setCategory(entityManager.find(Category.class, Long.valueOf(type.getValue())));
            }
        }
        var groups = categorySums.stream()
                .collect(Collectors.groupingBy(cs -> Pair.of(cs.getCurrency(), cs.getCategory())));
        return groups.entrySet().stream().map(e -> e.getValue().stream()
                .reduce(new CategorySum(e.getKey().getSecond(), e.getKey().getFirst(), .0), (a, g) -> {
                    a.setSum(a.getSum() + g.getSum());
                    return a;
                })).sorted((a, b) -> a.getCategory().getFullName().compareToIgnoreCase(b.getCategory().getFullName()))
                .toList();
    }

    public void saveImport(List<ImportDto> records, Long accountId, Long userId) {
        var owner = userRepository.findById(userId).orElseThrow();
        var account = accountRepository.findById(accountId).orElseThrow();
        var minOpdate = records.stream().filter(ImportDto::isSelected).map(r -> r.getOpdate())
                .min((a, b) -> a.compareTo(b)).orElse(LocalDateTime.now());
        var corrections = getCorrections(accountId, minOpdate, null);
        for (var dto : records) {
            if (dto.getId() != null && !dto.isSelected()) {
                var entity = transactionRepository.findById(dto.getId()).orElseThrow();
                if (entity.getParty() == null || entity.getParty().isBlank()) {
                    entity.setParty(dto.getParty());
                }
                if (entity.getDetails() == null || entity.getDetails().isBlank()) {
                    entity.setDetails(dto.getDetails());
                }
                entity.setOwner(owner);
                entity.setUpdated(LocalDateTime.now());
                transactionRepository.save(entity);
            } else if (dto.isSelected()) {
                var entity = new Transaction();
                entity.setOwner(owner);
                entity.setOpdate(dto.getOpdate());
                if (dto.getType() == TransactionType.EXPENSE) {
                    entity.setAccount(account);
                }
                entity.setDebit(dto.getDebit());
                if (dto.getType() == TransactionType.INCOME) {
                    entity.setRecipient(account);
                }
                entity.setCredit(dto.getCredit());
                if (dto.getCategory() != null) {
                    var category = categoryService.getCategory(dto.getCategory(), userId);
                    entity.setCategory(category);
                }
                entity.setCurrency(dto.getCurrency());
                entity.setParty(dto.getParty());
                entity.setDetails(dto.getDetails());
                entity.setUpdated(LocalDateTime.now());
                entity.setCreated(LocalDateTime.now());
                transactionRepository.save(entity);
                dto.setId(entity.getId());
                corrections.stream().filter(c -> c.getOpdate().isAfter(dto.getOpdate()))
                        .forEach(correction -> updateCorrection(correction, accountId,
                                dto.getType() == TransactionType.EXPENSE ? dto.getDebit() : -dto.getCredit()));
            }
        }
        corrections.stream().filter(c -> c.getCredit() == 0 && c.getDebit() == 0)
                .forEach(c -> transactionRepository.delete(c));
    }

    private void dto2entity(TransactionDto dto, Transaction entity) {
        entity.setOpdate(dto.opdate());
        if (dto.account() != null) {
            entity.setAccount(accountRepository.findById(dto.account().id()).orElseThrow());
        }
        entity.setDebit(dto.debit());
        if (dto.recipient() != null) {
            entity.setRecipient(accountRepository.findById(dto.recipient().id()).orElseThrow());
        }
        entity.setCredit(dto.credit());
        entity.setCurrency(dto.currency());
        entity.setParty(dto.party());
        entity.setDetails(dto.details());
        entity.setUpdated(LocalDateTime.now());
    }

    private void updateCorrections(Long accountId, Double amount, LocalDateTime opdate, Long id, Boolean removeZeros) {
        var corrections = getCorrections(accountId, opdate, id);
        for (var correction : corrections) {
            updateCorrection(correction, accountId, amount);
            if (removeZeros && correction.getCredit() == 0 && correction.getDebit() == 0) {
                transactionRepository.delete(correction);
            }
        }
    }

    private void updateCorrection(Transaction correction, Long accountId, Double amount) {
        if (correction.getAccount() != null && correction.getAccount().getId().equals(accountId)) {
            correction.setCredit(correction.getCredit() - amount);
            correction.setDebit(correction.getDebit() - amount);
            if (correction.getCredit() < 0) {
                correction.setCredit(-correction.getCredit());
                correction.setDebit(-correction.getDebit());
                correction.setRecipient(correction.getAccount());
                correction.setAccount(null);
            }
        } else if (correction.getRecipient() != null && correction.getRecipient().getId().equals(accountId)) {
            correction.setCredit(correction.getCredit() + amount);
            correction.setDebit(correction.getDebit() + amount);
            if (correction.getCredit() < 0) {
                correction.setCredit(-correction.getCredit());
                correction.setDebit(-correction.getDebit());
                correction.setAccount(correction.getRecipient());
                correction.setRecipient(null);
            }
        }
    }

    private List<Transaction> getCorrections(Long accountId, LocalDateTime opdate, Long id) {
        var builder = entityManager.getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(Transaction.class);
        var root = criteriaQuery.from(Transaction.class);
        var where = builder.or(builder.equal(root.get("account").get("id"), accountId),
                builder.equal(root.get("recipient").get("id"), accountId));
        where = builder.and(builder.equal(root.get("category").get("id"), 3L), where);
        if (id != null) {
            where = builder.and(where, builder.or(builder.greaterThan(root.<LocalDateTime>get("opdate"), opdate),
                    builder.and(builder.equal(root.<LocalDateTime>get("opdate"), opdate),
                            builder.greaterThan(root.get("id"), id))));
        } else {
            where = builder.and(where, builder.greaterThan(root.<LocalDateTime>get("opdate"), opdate));
        }
        criteriaQuery = criteriaQuery.where(where).orderBy(builder.asc(root.get("opdate")),
                builder.asc(root.get("id")));
        var typedQuery = entityManager.createQuery(criteriaQuery);
        return typedQuery.getResultList();
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
    
    boolean existsByAccountId(Long accountId) {
        return transactionRepository.existsByAccountIdOrRecipientId(accountId, accountId);
    }
}
