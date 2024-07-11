package com.swarmer.finance.repositories;

import java.util.stream.Stream;

import org.springframework.data.repository.CrudRepository;

import com.swarmer.finance.models.Acl;
import com.swarmer.finance.models.AclId;

public interface AclRepository extends CrudRepository<Acl, AclId> {
    Stream<Acl> findByUserId(Long userId);
    Stream<Acl> findByGroupOwnerIdOrderByGroupId(Long ownerId);
}
