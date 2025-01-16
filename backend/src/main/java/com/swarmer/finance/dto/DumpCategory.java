package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.swarmer.finance.models.Category;

public record DumpCategory(
        Long id,
        Long parentId,
        String name,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static DumpCategory from(Category category) {
        return new DumpCategory(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getCreated(),
                category.getUpdated()
        );
    }
}
