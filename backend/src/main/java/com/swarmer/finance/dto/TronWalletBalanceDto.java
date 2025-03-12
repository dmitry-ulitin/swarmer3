package com.swarmer.finance.dto;

import java.math.BigDecimal;

public record TronWalletBalanceDto(
    String address,
    BigDecimal trxBalance,
    BigDecimal usdtBalance
) {} 