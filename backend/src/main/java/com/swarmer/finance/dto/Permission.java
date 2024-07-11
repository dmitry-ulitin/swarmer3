package com.swarmer.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.Acl;

public record Permission(
    UserPrincipal user,
	@JsonProperty("is_admin") Boolean admin,
	@JsonProperty("is_readonly") Boolean readonly
) {
	public static Permission from(Acl acl) {
		return new Permission(UserPrincipal.from(acl.getUser()), acl.getAdmin(), acl.getReadonly());
	}
}
