package com.swarmer.finance.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.User;

public interface UserRepository extends CrudRepository<User, Long> {
	Optional<User> findByEmail(String email);
	List<User> findFirst10ByEmailnameContainingIgnoreCase(String query);
}