package com.swarmer.finance.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.Acl;

public record DumpAcl(
        @JsonProperty("user_id") Long userId,
        Boolean admin,
        Boolean readonly,
        String name,
        Boolean deleted,
        LocalDateTime created,
        LocalDateTime updated) {
    public static DumpAcl from(Acl acl) {
        return new DumpAcl(
                acl.getUser().getId(),
                acl.getAdmin(),
                acl.getReadonly(),
                acl.getName(),
                acl.getDeleted(),
                acl.getCreated(),
                acl.getUpdated());
    }

    public static DumpAcl mapUsers(DumpAcl acl, Map<Long, Long> userIds) {
        return new DumpAcl(
                userIds.get(acl.userId()),
                acl.admin(),
                acl.readonly(),
                acl.name(),
                acl.deleted(),
                acl.created(),
                acl.updated());
    }
}
