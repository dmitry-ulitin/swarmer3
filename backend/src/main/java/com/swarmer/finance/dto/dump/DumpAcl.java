package com.swarmer.finance.dto.dump;

import java.time.LocalDateTime;

import com.swarmer.finance.models.Acl;

public record DumpAcl(
        Long userId,
        Boolean admin,
        Boolean readonly,
        String name,
        LocalDateTime created,
        LocalDateTime updated) {
    public static DumpAcl fromEntity(Acl acl) {
        return new DumpAcl(
                acl.getUser().getId(),
                acl.isAdmin(),
                acl.isReadonly(),
                acl.getName(),
                acl.getCreated(),
                acl.getUpdated());
    }
}
