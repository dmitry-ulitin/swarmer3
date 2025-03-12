package com.swarmer.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionSum(Long accountId, Long recipientId, BigDecimal debit, BigDecimal credit,
        LocalDateTime opdate) {
}
