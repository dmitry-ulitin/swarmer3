package com.swarmer.finance.services;

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.swarmer.finance.models.Account;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AclRepository;

import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final AccountGroupRepository groupRepository;
    private final AclRepository aclRepository;
    private final TronService tronService;
    private final ImportService importService;
    private final TransactionService transactionService;

    public WalletService(AccountGroupRepository groupRepository, AclRepository aclRepository,
            TronService tronService, ImportService importService,
            TransactionService transactionService) {
        this.groupRepository = groupRepository;
        this.aclRepository = aclRepository;
        this.tronService = tronService;
        this.importService = importService;
        this.transactionService = transactionService;
    }

    @Transactional
    public void importWallets(Long userId, Collection<Long> accountIdsFilter) {
        var userGroups = groupRepository.findByOwnerIdOrderById(userId);
        var sharedGroups = aclRepository.findByUserIdOrderByGroupId(userId).stream()
                .map(acl -> acl.getGroup())
                .filter(group -> !group.getOwner().getId().equals(userId))
                .toList();
        var accounts = Stream.concat(userGroups.stream(), sharedGroups.stream())
                .flatMap(group -> group.getAccounts().stream())
                .filter(account -> !account.isDeleted())
                .filter(account -> account.getChain() != null && !account.getChain().isBlank())
                .filter(account -> account.getAddress() != null && !account.getAddress().isBlank())
                .filter(account -> (accountIdsFilter == null || accountIdsFilter.isEmpty() 
                    || accountIdsFilter.contains(account.getId())) && account.getChain() != null)
                .toList();
        
        for (var account : accounts) {
            importAccount(account, userId);
        }
    }

    @Transactional
    public void importAccount(Account account, Long userId) {
        if ("trc20".equals(account.getChain()) && account.getAddress() != null) {
            var balance = tronService.getWalletBalance(account.getAddress());
            if ("TRX".equalsIgnoreCase(account.getCurrency())) {
                var records = tronService.getTrxTransactions(balance.hexAddress());
                importService.importRecords(records, account.getId(), userId);
                transactionService.saveImport(userId, account.getId(), records);
            } else {
                var records = tronService.getContractTransactions(balance.address()).stream()
                        .filter(r -> r.getCurrency().equals(account.getCurrency())).toList();
                importService.importRecords(records, account.getId(), userId);
                transactionService.saveImport(userId, account.getId(), records);
            }
        }
    }
}
