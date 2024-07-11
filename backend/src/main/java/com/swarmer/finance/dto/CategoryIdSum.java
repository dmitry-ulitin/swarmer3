package com.swarmer.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryIdSum {
    Long id;
    String currency;
    Double amount;
}
