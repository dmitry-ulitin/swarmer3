package com.swarmer.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swarmer.finance.models.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportDto {
    private Long id;
    private LocalDateTime opdate;
    private TransactionType type;
    private BigDecimal debit;
    private BigDecimal credit;
    private RuleDto rule;
    private CategoryDto category;
    private String currency;
    private String party;
    private String details;
    private String catname;
    private boolean selected;
}
