package com.swarmer.finance.dto;

public record UserToCreate(String email, String password, String name, String currency) {
}
