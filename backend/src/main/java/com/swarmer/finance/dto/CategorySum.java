package com.swarmer.finance.dto;

import java.math.BigDecimal;

import com.swarmer.finance.models.Category;

import lombok.Data;

@Data
public class CategorySum {
    CategoryDto category;
    String currency;
    BigDecimal sum;

    public CategorySum(Category category, String currency, BigDecimal sum) {
        this.category = CategoryDto.fromEntity(category);
        this.currency = currency;
        this.sum = sum;
    }

    public CategorySum(CategoryDto category, String currency, BigDecimal sum) {
        this.category = category;
        this.currency = currency;
        this.sum = sum;
    }
}
