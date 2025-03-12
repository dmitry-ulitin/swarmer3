package com.swarmer.finance.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swarmer.finance.models.Acl;
import com.swarmer.finance.models.AclId;

@Repository
public interface AclRepository extends JpaRepository<Acl, AclId> {
    List<Acl> findByUserIdOrderByGroupId(Long userId);
} 