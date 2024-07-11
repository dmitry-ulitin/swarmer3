package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportDto {
    private Long id;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[XXX]")
    private LocalDateTime opdate;
    private TransactionType type;
    private Double debit;
    private Double credit;
    private RuleDto rule;
    private Category category;
    private String currency;
    private String party;
    private String details;
    private boolean selected;
}