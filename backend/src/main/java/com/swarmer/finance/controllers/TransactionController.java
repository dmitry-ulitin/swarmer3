package com.swarmer.finance.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.swarmer.finance.dto.CategorySum;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.RuleDto;
import com.swarmer.finance.dto.Summary;
import com.swarmer.finance.dto.TransactionDto;
import com.swarmer.finance.models.BankType;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.security.UserPrincipal;
import com.swarmer.finance.services.ImportService;
import com.swarmer.finance.services.TransactionService;
import com.swarmer.finance.services.WalletService;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final ImportService importService;
    private final WalletService walletService;

    @Autowired
    public TransactionController(TransactionService transactionService, ImportService importService,
            WalletService walletService) {
        this.transactionService = transactionService;
        this.importService = importService;
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Set<Long> accounts,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(transactionService.getTransactions(userId, accounts, search, category, currency,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.atTime(LocalTime.MAX), offset,
                limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(transactionService.getTransaction(id, userId));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            @RequestBody TransactionDto transaction,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(transactionService.createTransaction(transaction, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @PathVariable Long id,
            @RequestBody TransactionDto transaction,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        // Ensure the ID in the path matches the transaction
        if (!id.equals(transaction.id())) {
            throw new IllegalArgumentException("Transaction ID mismatch");
        }
        return ResponseEntity.ok(transactionService.updateTransaction(transaction, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Collection<Summary>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Set<Long> accounts,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(transactionService.getSummary(userId, accounts,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.atTime(LocalTime.MAX)));
    }

    @GetMapping("categories")
    public ResponseEntity<Collection<CategorySum>> getSummaryByCategories(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Set<Long> accounts,
            @RequestParam(required = false, defaultValue = "1") int type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity
                .ok(transactionService.getCategoriesSummary(userId, TransactionType.fromValue(type), accounts,
                        from == null ? null : from.atStartOfDay(), to == null ? null : to.atTime(LocalTime.MAX)));
    }

    @GetMapping("rules")
    public ResponseEntity<Collection<RuleDto>> getRules(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(importService.getRules(userId));
    }

    @PostMapping("rules")
    public ResponseEntity<RuleDto> createRule(
            @RequestBody RuleDto rule,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(importService.createRule(rule, userId));
    }

    @PutMapping("rules/{id}")
    public ResponseEntity<RuleDto> updateRule(
            @PathVariable Long id,
            @RequestBody RuleDto rule,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        // Ensure the ID in the path matches the rule
        if (!id.equals(rule.id())) {
            throw new IllegalArgumentException("Rule ID mismatch");
        }
        return ResponseEntity.ok(importService.updateRule(id, rule, userId));
    }

    @DeleteMapping("rules/{id}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        importService.deleteRule(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("import")
    public ResponseEntity<List<ImportDto>> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "1") int bank,
            @RequestParam Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        try (InputStream inputStream = file.getInputStream()) {
            return ResponseEntity.ok(importService.importFile(inputStream, BankType.fromValue(bank), id, userId));
        } catch (IOException | ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad input file", e);
        }
    }

    @PatchMapping("import")
    public ResponseEntity<Void> importTransactions(
            @RequestBody List<ImportDto> records,
            @RequestParam Long account,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        transactionService.saveImport(userId, account, records);
        return ResponseEntity.ok().build();
    }

    @GetMapping("checkwallets")
    public ResponseEntity<Long> checkWallets(@RequestParam(required = false) Set<Long> accounts,
            @RequestParam(required = false, defaultValue = "false") boolean fullScan,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserDto().id();
        return ResponseEntity.ok(walletService.importWallets(userId, accounts, fullScan));
    }
}