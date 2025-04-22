package com.swarmer.finance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.swarmer.finance.dto.BitcoinWalletBalanceDto;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.models.Transaction;
import com.swarmer.finance.models.TransactionType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BitcoinService {
    private static final BigDecimal BTC_DECIMALS = new BigDecimal("100000000"); // 8 decimals (satoshi)
    private static final String BITCOIN_NETWORK = "btc"; // Bitcoin main network
    private static final String API_VERSION = "v1";
    private static final int LIMIT = 2000;

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public BitcoinService(
            @Value("${bitcoin.api.url:https://api.blockcypher.com}") String apiUrl,
            @Value("${bitcoin.api.key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Get Bitcoin wallet balance for a given address
     * 
     * @param address Bitcoin wallet address
     * @return BitcoinWalletBalanceDto with wallet information
     */
    public BitcoinWalletBalanceDto getWalletBalance(String address) {
        try {
            // Build API URL with optional API key

            String url = apiUrl + "/" + API_VERSION + "/" + BITCOIN_NETWORK + "/main/addrs/" + address + "/balance";
            if (!apiKey.isEmpty()) {
                url += "?token=" + apiKey;
            }
            // Send the request
            var response = restTemplate.getForObject(url, JsonNode.class);

            BigDecimal btcBalance = BigDecimal.ZERO;
            if (response != null) {
                // BlockCypher returns balance in satoshis
                if (response.has("final_balance")) {
                    BigDecimal satoshiBalance = new BigDecimal(response.get("final_balance").asText());
                    btcBalance = satoshiBalance.divide(BTC_DECIMALS, 8, RoundingMode.HALF_DOWN);
                }
            }
            return new BitcoinWalletBalanceDto(address, btcBalance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Bitcoin wallet balance: " + e.getMessage(), e);
        }
    }

    /**
     * Get Bitcoin transactions for a wallet address
     * 
     * @param address  Bitcoin wallet address
     * @param lastTransaction last transaction for pagination
     * @return List of ImportDto with transaction information
     */
    public List<ImportDto> getTransactions(String address, Optional<Transaction> lastTransaction) {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }

        try {
            List<ImportDto> result = new ArrayList<>();
            long beforeBlock = -1;
            long afterBlock = -1;
            boolean hasMore = true;

            if (lastTransaction != null && lastTransaction.isPresent()) {
                Pattern pattern = Pattern.compile("block_height: (\\d+)");
                Matcher matcher = pattern.matcher(lastTransaction.get().getDetails());
                if (matcher.find()) {
                    afterBlock = Long.parseLong(matcher.group(1));
                }
            }

            while (hasMore) {
                // Build the URL with pagination parameters
                String url = apiUrl + "/" + API_VERSION + "/" + BITCOIN_NETWORK + "/main/addrs/" + address;
                url += "?limit=" + LIMIT;
                if (!apiKey.isEmpty()) {
                    url += "&token=" + apiKey;
                }

                if (afterBlock >= 0) {
                    url += "&after=" + afterBlock;
                }
                if (beforeBlock >= 0) {
                    url += "&before=" + beforeBlock;
                }

                // Send the request
                var response = restTemplate.getForObject(url, JsonNode.class);

                if (response != null && response.has("txrefs")) {
                    JsonNode transactions = response.get("txrefs");

                    // Process each transaction
                    for (JsonNode tx : transactions) {
                        // Get the transaction hash for pagination and details
                        beforeBlock = tx.get("block_height").asLong();
                        // Process inputs and outputs to determine transaction direction and amount
                        BigDecimal satoshi = new BigDecimal(tx.get("value").asText());
                        BigDecimal amount = satoshi.divide(BTC_DECIMALS, 8, RoundingMode.HALF_DOWN);
                        Boolean spent = tx.get("spent").asBoolean();
                        LocalDateTime confirmed =LocalDateTime.parse(tx.get("confirmed").asText(), DateTimeFormatter.ISO_DATE_TIME);
                        String txHash = tx.get("tx_hash").asText();
                        var details = "tx_hash: " + txHash + ", block_height: " + beforeBlock;


                        result.add(new ImportDto(
                                null, // id
                                confirmed,
                                spent ? TransactionType.EXPENSE : TransactionType.INCOME,
                                amount, // debit
                                amount, // credit
                                null, // rule
                                null, // category
                                "BTC", // currency
                                null, // party
                                details, // details
                                null, // catname
                                false // selected
                        ));
                    }
                    hasMore = response.has("hasMore") && response.get("hasMore").asBoolean();
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Bitcoin transactions: " + e.getMessage(), e);
        }
    }
}
