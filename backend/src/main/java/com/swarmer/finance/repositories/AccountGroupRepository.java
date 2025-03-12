package com.swarmer.finance.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.AccountGroup;

import java.util.List;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long> {
    List<AccountGroup> findByOwnerIdOrderById(Long ownerId);
}
