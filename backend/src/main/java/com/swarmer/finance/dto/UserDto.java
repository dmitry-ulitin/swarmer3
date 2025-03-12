package com.swarmer.finance.dto;

import com.swarmer.finance.models.User;

public record UserDto(
        Long id,
        String email,
        String name,
        String currency) {
    // Convert User entity to UserDto
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCurrency());
    }
}