package com.swarmer.finance.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.AccountGroup;

public interface GroupRepository extends CrudRepository<AccountGroup, Long> {
    List<AccountGroup> findByOwnerIdInOrderById(Collection<Long> ids);

    @Modifying
    @Query("update account_groups g set g.deleted = true where g.owner.id = ?1")
    int deleteByOwnerId(Long ownerId);
}
