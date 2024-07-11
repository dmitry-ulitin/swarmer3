package com.swarmer.finance.services;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.Acl;
import com.swarmer.finance.repositories.AclRepository;
import com.swarmer.finance.repositories.GroupRepository;

@Service
public class AclService {
    private final AclRepository aclRepository;
    private final GroupRepository groupRepository;

    public AclService(AclRepository aclRepository, GroupRepository groupRepository) {
        this.aclRepository = aclRepository;
        this.groupRepository = groupRepository;
    }

    public Stream<Acl> findByUserId(Long userId) {
        return aclRepository.findByUserId(userId);
    }

    public void delete(Acl acl) {
        aclRepository.delete(acl);
    }

    public List<Long> findUsers(Long userId) {
        return Stream.concat(
                Stream.concat(
                        aclRepository.findByUserId(userId).map(a -> a.getGroup().getOwner().getId()),
                        aclRepository.findByGroupOwnerIdOrderByGroupId(userId).map(a -> a.getUser().getId())),
                Stream.of(userId))
                .distinct().toList();
    }

    public Stream<Account> findAccounts(Long userId) {
        return Stream.concat(
                aclRepository.findByUserId(userId).flatMap(a -> a.getGroup().getAccounts().stream()),
                groupRepository.findByOwnerIdInOrderById(List.of(userId)).stream().flatMap(g -> g.getAccounts().stream()));
    }
}
