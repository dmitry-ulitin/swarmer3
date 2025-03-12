package com.swarmer.finance.dto;

import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.TransactionType;

public record CategoryDto(
        Long id,
        Long ownerId,
        Long parentId, // We'll use parentId instead of the full Category object
        String name,
        String fullName,
        TransactionType type,
        int level) {
    public static CategoryDto fromEntity(Category entity) {
        if (entity == null) {
            return null;
        }
        var root = entity;
        var level = 0;
        var fullName = entity.getName();
        while (root.getParent() != null) {
            root = root.getParent();
            level++;
            if (root.getParent() != null) {
                fullName = root.getName() + " / " + fullName;
            }
        }
        var parentId = entity.getParent() == null ? null : entity.getParent().getId();
        return new CategoryDto(entity.getId(), entity.getOwnerId(), parentId, entity.getName(),
                fullName, TransactionType.fromValue(root.getId().intValue()), level);
    }
}