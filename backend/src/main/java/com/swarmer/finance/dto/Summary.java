package com.swarmer.finance.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Summary {
    private String currency;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal transfers_debit;
    private BigDecimal transfers_credit;
}
