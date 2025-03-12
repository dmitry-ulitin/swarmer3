package com.swarmer.finance.dto.dump;

import java.time.LocalDateTime;
import java.util.List;

import com.swarmer.finance.models.AccountGroup;

public record DumpGroup(
        Long id,
        List<DumpAcl> acls,
        List<DumpAccount> accounts,
        String name,
        Boolean deleted,
        LocalDateTime created,
        LocalDateTime updated) {
    public static DumpGroup fromEntity(AccountGroup group) {
        return new DumpGroup(
                group.getId(),
                group.getAcls().stream().map(DumpAcl::fromEntity).toList(),
                group.getAccounts().stream().map(DumpAccount::fromEntity).toList(),
                group.getName(),
                group.isDeleted(),
                group.getCreated(),
                group.getUpdated());
    }
}
