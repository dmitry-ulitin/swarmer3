package com.swarmer.finance.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOwnerIdIsNullOrOwnerIdIn(Collection<Long> ids);

    List<Category> findAllByOwnerIdAndParentIdAndNameIgnoreCase(Long ownerId, Long parentId, String name);

    void deleteAllByOwnerIdIsNotNull();

    void deleteAllByOwnerId(Long ownerId);
    
    @Modifying
    @Query("update Category set parent.id = ?2 where parent.id = ?1")
    int replaceParentId(Long oldId, Long newId);
}