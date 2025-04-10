package com.swarmer.finance.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.AccountAddress;
import java.util.List;

@Repository
public interface AccountAddressRepository extends JpaRepository<AccountAddress, Long> {
    List<AccountAddress> findByAccountIdIn(List<Long> accountIds);
} 