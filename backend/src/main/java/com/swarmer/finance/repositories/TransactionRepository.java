package com.swarmer.finance.repositories;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.swarmer.finance.models.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByAccountIdOrRecipientId(Long accountId, Long recipientId);

    Stream<Transaction> findAllByOwnerId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from transactions where owner.id = ?1")
    void removeByOwnerId(Long userId);

    @Modifying
    @Query("update transactions t set t.category.id = ?2 where t.category.id = ?1")
    int replaceCategoryId(Long categoryId, Long replaceId);
}
