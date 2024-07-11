package com.swarmer.finance.dto;

import com.swarmer.finance.models.User;

public record UserPrincipal(Long id, String email, String name, String currency) {
    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getName(), user.getCurrency());
    }
}
