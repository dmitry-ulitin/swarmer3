package com.swarmer.finance.dto;

import com.swarmer.finance.models.Acl;

public record Permission(
    UserPrincipal user,
	Boolean admin,
	Boolean readonly
) {
	public static Permission from(Acl acl) {
		return new Permission(UserPrincipal.from(acl.getUser()), acl.getAdmin(), acl.getReadonly());
	}
}
