package com.swarmer.finance.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOwnerId(Long userId);

    List<Transaction> findByAccountIdInOrRecipientIdIn(Collection<Long> aIds, Collection<Long> rIds);

    void deleteAllByOwnerId(Long userId);

    @Modifying
    @Query("update Transaction set category.id = ?2 where category.id = ?1")
    int replaceCategoryId(Long oldId, Long newId);
}