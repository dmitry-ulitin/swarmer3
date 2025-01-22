package com.swarmer.finance.dto;

import com.swarmer.finance.models.Category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorySum {
    Category category;
    String currency;
    Double sum;
}
