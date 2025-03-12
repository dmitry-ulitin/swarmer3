package com.swarmer.finance.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Find user by email
    Optional<User> findByEmailIgnoreCase(String email);
    
    // Check if user exists by email
    boolean existsByEmailIgnoreCase(String email);

    // Find first 10 users by emailname
    List<User> findFirst10ByEmailnameContainingIgnoreCase(String query);
} 