package com.swarmer.finance.dto;

import java.math.BigDecimal;

public record BitcoinWalletBalanceDto(
    String address,
    BigDecimal btcBalance
) {}
