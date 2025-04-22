package com.swarmer.finance.dto;

import java.math.BigDecimal;

public record TronWalletBalanceDto(
    String address,
    String hexAddress,
    BigDecimal trxBalance,
    BigDecimal usdtBalance
) {} 