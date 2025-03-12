package com.swarmer.finance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.swarmer.finance.dto.TronWalletBalanceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class TronService {
    private static final String USDT_CONTRACT_ADDRESS = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    private static final BigDecimal TRX_DECIMALS = new BigDecimal("1000000"); // 6 decimals
    private static final BigDecimal USDT_DECIMALS = new BigDecimal("1000000"); // 6 decimals

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public TronService(
            @Value("${tron.api.url:https://api.trongrid.io}") String apiUrl,
            @Value("${tron.api.key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public TronWalletBalanceDto getWalletBalance(String address) {
        try {
            // Get TRX balance
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!apiKey.isEmpty()) {
                headers.set("TRON-PRO-API-KEY", apiKey);
            }

            // Get TRX balance
            var accountResponse = restTemplate.getForObject(
                    apiUrl + "/v1/accounts/" + address,
                    JsonNode.class);

            BigDecimal trxBalance = BigDecimal.ZERO;
            if (accountResponse != null && accountResponse.has("data")) {
                var data = accountResponse.get("data").get(0);
                if (data.has("balance")) {
                    trxBalance = new BigDecimal(data.get("balance").asText())
                            .divide(TRX_DECIMALS, 6, RoundingMode.HALF_DOWN);
                }
            }

            // Get USDT balance using smart contract
            var contractRequest = Map.of(
                    "owner_address", address,
                    "contract_address", USDT_CONTRACT_ADDRESS,
                    "function_selector", "balanceOf(address)",
                    "parameter", address.substring(2) // Remove "0x" prefix
            );

            var contractResponse = restTemplate.postForObject(
                    apiUrl + "/wallet/triggersmartcontract",
                    new HttpEntity<>(contractRequest, headers),
                    JsonNode.class);

            BigDecimal usdtBalance = BigDecimal.ZERO;
            if (contractResponse != null && contractResponse.has("constant_result")) {
                var result = contractResponse.get("constant_result").get(0).asText();
                var usdtRaw = new BigDecimal(Long.parseLong(result, 16));
                usdtBalance = usdtRaw.divide(USDT_DECIMALS, 6, RoundingMode.HALF_DOWN);
            }

            return new TronWalletBalanceDto(address, trxBalance, usdtBalance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get wallet balance: " + e.getMessage(), e);
        }
    }
} 