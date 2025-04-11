package com.swarmer.finance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.swarmer.finance.dto.TronWalletBalanceDto;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.models.TransactionType;

import org.apache.commons.codec.digest.DigestUtils;
import org.bitcoinj.base.Base58;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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

            var accountResponse = restTemplate.getForObject(
                    apiUrl + "/v1/accounts/" + address,
                    JsonNode.class);

            BigDecimal trxBalance = BigDecimal.ZERO;
            BigDecimal usdtBalance = BigDecimal.ZERO;
            String hexAddress = address;
            if (accountResponse != null && accountResponse.has("data")) {
                var data = accountResponse.get("data").get(0);
                if (data.has("balance")) {
                    trxBalance = new BigDecimal(data.get("balance").asText())
                            .divide(TRX_DECIMALS, 6, RoundingMode.HALF_DOWN);
                }
                if (data.has("address")) {
                    hexAddress = data.get("address").asText();
                }
                if (data.has("trc20")) {
                    var trc20 = data.get("trc20");
                    for (JsonNode node : trc20) {
                        if (node.has(USDT_CONTRACT_ADDRESS)) {
                            usdtBalance = new BigDecimal(node.get(USDT_CONTRACT_ADDRESS).asText())
                                    .divide(USDT_DECIMALS, 6, RoundingMode.HALF_DOWN);
                        }
                    }
                }
            }

            return new TronWalletBalanceDto(address, hexAddress, trxBalance, usdtBalance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get wallet balance: " + e.getMessage(), e);
        }
    }

    public List<ImportDto> getTrxTransactions(String address) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!apiKey.isEmpty()) {
                headers.set("TRON-PRO-API-KEY", apiKey);
            }

            var transactionsResponse = restTemplate.getForObject(
                    apiUrl + "/v1/accounts/" + address + "/transactions",
                    JsonNode.class);

            List<ImportDto> result = new ArrayList<>();
            if (transactionsResponse != null && transactionsResponse.has("data")) {
                for (JsonNode transaction : transactionsResponse.get("data")) {
                    if (transaction.has("raw_data") && transaction.get("raw_data").has("contract")) {
                        var contract = transaction.get("raw_data").get("contract").get(0);
                        if (contract.has("parameter") && contract.get("parameter").has("value")) {
                            var value = contract.get("parameter").get("value");
                            if (value.has("amount")) {
                                var amount = new BigDecimal(value.get("amount").asText())
                                        .divide(TRX_DECIMALS, 6, RoundingMode.HALF_DOWN)
                                        .setScale(2, RoundingMode.HALF_DOWN);
                                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                                    continue;
                                }
                                var timestamp = transaction.get("raw_data").get("timestamp").asLong();
                                var opdate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                                        ZoneId.systemDefault());

                                // Determine if this is an incoming or outgoing transaction
                                var isIncoming = value.has("to_address") &&
                                        value.get("to_address").asText().equalsIgnoreCase(address);
                                var party = isIncoming ? value.get("owner_address").asText()
                                        : value.get("to_address").asText();

                                result.add(new ImportDto(
                                        null, // id
                                        opdate,
                                        isIncoming ? TransactionType.INCOME : TransactionType.EXPENSE,
                                        amount, // debit
                                        amount, // credit
                                        null, // rule
                                        null, // category
                                        "TRX", // currency
                                        encode58(party), // party
                                        "TRX Transaction", // details
                                        null, // catname
                                        false // selected
                                ));
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get TRX transactions: " + e.getMessage(), e);
        }
    }

    public List<ImportDto> getContractTransactions(String address) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!apiKey.isEmpty()) {
                headers.set("TRON-PRO-API-KEY", apiKey);
            }

            var transactionsResponse = restTemplate.getForObject(
                    apiUrl + "/v1/accounts/" + address + "/transactions/trc20",
                    JsonNode.class);

            List<ImportDto> result = new ArrayList<>();
            if (transactionsResponse != null && transactionsResponse.has("data")) {
                for (JsonNode transaction : transactionsResponse.get("data")) {
                    if (transaction.has("value") && transaction.has("token_info")) {
                        var tokenInfo = transaction.get("token_info");
                        var amount = new BigDecimal(transaction.get("value").asText())
                                .divide(USDT_DECIMALS, 6, RoundingMode.HALF_DOWN)
                                .setScale(2, RoundingMode.HALF_DOWN);
                        if (amount.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }

                        var timestamp = transaction.get("block_timestamp").asLong();
                        var opdate = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault());

                        // Determine if this is an incoming or outgoing transaction
                        var isIncoming = transaction.has("to") && 
                            transaction.get("to").asText().equalsIgnoreCase(address);
                        var party = isIncoming ? transaction.get("from").asText() : 
                            transaction.get("to").asText();

                        result.add(new ImportDto(
                                null, // id
                                opdate,
                                isIncoming ? TransactionType.INCOME : TransactionType.EXPENSE,
                                amount, // debit
                                amount, // credit
                                null, // rule
                                null, // category
                                tokenInfo.get("symbol").asText(), // currency
                                party, // party
                                tokenInfo.get("name").asText() + " Transaction", // details
                                null, // catname
                                false // selected
                        ));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get contract transactions: " + e.getMessage(), e);
        }
    }

    private String encode58(String hexString) {
        byte[] input = Hex.decode(hexString);
        byte[] hash0 = DigestUtils.sha256(input);
        byte[] hash1 = DigestUtils.sha256(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }
}