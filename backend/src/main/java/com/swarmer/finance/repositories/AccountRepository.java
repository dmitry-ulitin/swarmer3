package com.swarmer.finance.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swarmer.finance.models.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
