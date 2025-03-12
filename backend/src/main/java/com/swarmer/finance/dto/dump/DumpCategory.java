package com.swarmer.finance.dto.dump;

import java.time.LocalDateTime;

import com.swarmer.finance.models.Category;

public record DumpCategory(
                Long id,
                Long parentId,
                String name,
                LocalDateTime created,
                LocalDateTime updated) {
        public static DumpCategory fromEntity(Category category) {
                return new DumpCategory(
                                category.getId(),
                                category.getParent() == null ? null : category.getParent().getId(),
                                category.getName(),
                                category.getCreated(),
                                category.getUpdated());
        }
}
