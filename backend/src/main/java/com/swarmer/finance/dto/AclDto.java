package com.swarmer.finance.dto;

import com.swarmer.finance.models.Acl;

public record AclDto(
        UserDto user,
        boolean readonly,
        boolean admin) {
    public static AclDto fromEntity(Acl entity) {
        return new AclDto(
                UserDto.fromEntity(entity.getUser()),
                entity.isReadonly(),
                entity.isAdmin());
    }
}