package com.swarmer.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Summary{
    private String currency;
    private Double debit;
    private Double credit;
    private Double transfers_debit;
    private Double transfers_credit;
}
