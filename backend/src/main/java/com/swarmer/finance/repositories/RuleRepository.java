package com.swarmer.finance.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.Rule;

public interface RuleRepository extends CrudRepository<Rule, Long> {
    List<Rule> findAllByOwnerId(Long ownerId);

    @Modifying(clearAutomatically = true)
    @Query("delete from rules where ownerId = ?1")
    void removeByOwnerId(Long ownerId);

    @Modifying
    @Query("delete from rules where category.id = ?1")
    void removeByCategoryId(Long categoryId);

    @Modifying
    @Query("update rules r set r.category.id = ?2 where r.category.id = ?1")
    int replaceCategoryId(Long categoryId, Long replaceId);
}
