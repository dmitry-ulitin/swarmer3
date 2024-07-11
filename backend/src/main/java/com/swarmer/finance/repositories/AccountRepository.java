package com.swarmer.finance.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.Account;

public interface AccountRepository extends CrudRepository<Account, Long> {
    List<Account> findByGroupId(Long id);
    List<Account> findByIdIn(Collection<Long> ids);
}
