package com.swarmer.finance.services;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.swarmer.finance.models.Account;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AclRepository;

@Service
public class AclService {
    private final AclRepository aclRepository;
    private final AccountGroupRepository groupRepository;

    public AclService(AclRepository aclRepository, AccountGroupRepository groupRepository) {
        this.aclRepository = aclRepository;
        this.groupRepository = groupRepository;
    }

    public List<Account> getAccounts(Long userId) {
        var userGroups = groupRepository.findByOwnerIdOrderById(userId);
        var sharedGroups = aclRepository.findByUserIdOrderByGroupId(userId).stream()
                .map(acl -> acl.getGroup())
                .filter(group -> !group.getOwner().getId().equals(userId))
                .toList();
        return Stream.concat(userGroups.stream(), sharedGroups.stream())
                .flatMap(group -> group.getAccounts().stream())
                .toList();
    }
}
