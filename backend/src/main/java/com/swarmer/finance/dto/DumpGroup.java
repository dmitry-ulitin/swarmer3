package com.swarmer.finance.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.swarmer.finance.models.AccountGroup;

public record DumpGroup(
        Long id,
        List<DumpAcl> acls,
        List<DumpAccount> accounts,
        String name,
        Boolean deleted,
        LocalDateTime created,
        LocalDateTime updated) {
    public static DumpGroup from(AccountGroup group) {
        return new DumpGroup(
                group.getId(),
                group.getAcls().stream().map(DumpAcl::from).toList(),
                group.getAccounts().stream().map(DumpAccount::from).toList(),
                group.getName(),
                group.getDeleted(),
                group.getCreated(),
                group.getUpdated());
    }

    public static DumpGroup mapUsers(DumpGroup group, Map<Long, Long> userIds) {
        return new DumpGroup(
                group.id(),
                group.acls().stream().map(acl -> DumpAcl.mapUsers(acl, userIds)).toList(),
                group.accounts(),
                group.name(),
                group.deleted(),
                group.created(),
                group.updated());
    }
}
