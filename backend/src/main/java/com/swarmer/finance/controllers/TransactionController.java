package com.swarmer.finance.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.swarmer.finance.dto.CategorySum;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.RuleDto;
import com.swarmer.finance.dto.Summary;
import com.swarmer.finance.dto.TransactionDto;
import com.swarmer.finance.dto.UserPrincipal;
import com.swarmer.finance.models.BankType;
import com.swarmer.finance.models.TransactionTypeConverter;
import com.swarmer.finance.services.ImportService;
import com.swarmer.finance.services.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@Transactional
public class TransactionController {
    private final TransactionService transactionService;
    private final ImportService importService;

    public TransactionController(TransactionService transactionService, ImportService importService) {
        this.transactionService = transactionService;
        this.importService = importService;
    }

    @GetMapping
    List<TransactionDto> getTransactions(Authentication authentication,
            @RequestParam(required = false, defaultValue = "") Collection<Long> accounts,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") Long category,
            @RequestParam(required = false, defaultValue = "") String currency,
            @RequestParam(required = false, defaultValue = "") String from,
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "0") int limit) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        LocalDateTime fromDate = from.isBlank() ? null : LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDate = to.isBlank() ? null : LocalDate.parse(to).atTime(LocalTime.MAX);
        return transactionService.getTransactions(userId, accounts, search, category, currency, fromDate, toDate,
                offset, limit);
    }

    @GetMapping("/{id}")
    TransactionDto getTransaction(@PathVariable("id") Long id, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return transactionService.getTransaction(id, userId);
    }

    @PostMapping
    TransactionDto createTransaction(@RequestBody TransactionDto group, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return transactionService.createTransaction(group, userId);
    }

    @PutMapping
    TransactionDto updateTransaction(@RequestBody TransactionDto group, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return transactionService.updateTransaction(group, userId);
    }

    @DeleteMapping("/{id}")
    void deleteTransaction(@PathVariable("id") Long id, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        transactionService.deleteTransaction(id, userId);
    }

    @GetMapping("/summary")
    Collection<Summary> getSummary(Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String from,
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestParam(required = false, defaultValue = "") Collection<Long> accounts) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        LocalDateTime fromDate = from.isBlank() ? null : LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDate = to.isBlank() ? null : LocalDate.parse(to).atTime(LocalTime.MAX);
        return transactionService.getSummary(userId, accounts, fromDate, toDate);
    }

    @GetMapping("/categories")
    Collection<CategorySum> getCategoriesSummary(Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") Long type,
            @RequestParam(required = false, defaultValue = "") String from,
            @RequestParam(required = false, defaultValue = "") String to,
            @RequestParam(required = false, defaultValue = "") Collection<Long> accounts) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        LocalDateTime fromDate = from.isBlank() ? null : LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDate = to.isBlank() ? null : LocalDate.parse(to).atTime(LocalTime.MAX);
        return transactionService.getCategoriesSummary(userId,
                new TransactionTypeConverter().convertToEntityAttribute(type), accounts, fromDate, toDate);
    }

    @PostMapping("/import")
    List<ImportDto> importFile(@RequestParam("file") MultipartFile file, @RequestParam("id") Long accountId,
            @RequestParam("bank") Long bankId, Authentication authentication) {
        try {
            var userId = ((UserPrincipal) authentication.getPrincipal()).id();
            var bank = BankType.fromValue(bankId);
            return importService.importFile(file.getInputStream(), bank, accountId, userId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad input file", e);
        }
    }

    @PatchMapping("/import")
    void saveImport(@RequestBody List<ImportDto> records, @RequestParam("account") Long accountId,
            Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        transactionService.saveImport(records, accountId, userId);
    }

    @GetMapping("/rules")
    List<RuleDto> getRules(Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return importService.getRules(userId);
    }

    @GetMapping("/rules/{id}")
    RuleDto getRule(@PathVariable("id") Long id, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return importService.getRuleById(id, userId);
    }

    @PostMapping("/rules")
    RuleDto addRule(@RequestBody RuleDto rule, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return importService.addRule(rule, userId);
    }

    @PutMapping("/rules")
    RuleDto updateRule(@RequestBody RuleDto rule, Authentication authentication) {
        var userId = ((UserPrincipal) authentication.getPrincipal()).id();
        return importService.updateRule(rule, userId);
    }
}
