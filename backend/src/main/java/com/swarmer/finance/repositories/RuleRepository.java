package com.swarmer.finance.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.Rule;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findAllByOwnerId(Long ownerId);
    
    void deleteAllByOwnerId(Long ownerId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Rule where category.id = ?1")
    void removeByCategoryId(Long categoryId);

    @Modifying
    @Query("update Rule set category.id = ?2 where category.id = ?1")
    int replaceCategoryId(Long oldId, Long newId);
}