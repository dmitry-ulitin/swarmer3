package com.swarmer.finance.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.Category;

public interface CategoryRepository extends CrudRepository<Category, Long> {
    List<Category> findByOwnerIdIsNullOrOwnerIdIn(Collection<Long> ids);

    List<Category> findAllByOwnerIdAndParentIdAndNameIgnoreCase(Long ownerId, Long parentId, String name);

    @Modifying(clearAutomatically = true)
    @Query("delete from categories where ownerId = ?1")
    void removeByOwnerId(Long ownerId);

    @Modifying
    @Query("update categories set parentId = ?2 where parentId = ?1")
    int replaceParentId(Long categoryId, Long replaceId);
}
