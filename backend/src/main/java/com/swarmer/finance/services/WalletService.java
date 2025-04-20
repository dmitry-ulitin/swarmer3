package com.swarmer.finance.services;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AclRepository;

import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final AccountGroupRepository groupRepository;
    private final AclRepository aclRepository;
    private final TronService tronService;
    private final BitcoinService bitcoinService;
    private final ImportService importService;
    private final TransactionService transactionService;

    public WalletService(AccountGroupRepository groupRepository, AclRepository aclRepository,
            TronService tronService, BitcoinService bitcoinService, ImportService importService,
            TransactionService transactionService) {
        this.groupRepository = groupRepository;
        this.aclRepository = aclRepository;
        this.tronService = tronService;
        this.bitcoinService = bitcoinService;
        this.importService = importService;
        this.transactionService = transactionService;
    }

    @Transactional
    public long importWallets(Long userId, Collection<Long> accountIdsFilter, boolean fullScan) {
        long count = 0;
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
            count += importAccount(account, userId, fullScan);
        }
        return count;
    }

    private long importAccount(Account account, Long userId, boolean fullScan) {
        List<ImportDto> records = null;
        if ("trc20".equals(account.getChain()) && account.getAddress() != null) {
            var balance = tronService.getWalletBalance(account.getAddress());
            if ("TRX".equalsIgnoreCase(account.getCurrency())) {
                records = tronService.getTrxTransactions(balance.hexAddress(), fullScan);
            } else {
                records = tronService.getContractTransactions(balance.address(), fullScan).stream()
                        .filter(r -> r.getCurrency().equals(account.getCurrency())).toList();
            }
        } else if ("btc".equals(account.getChain()) && account.getAddress() != null) {
            var balance = bitcoinService.getWalletBalance(account.getAddress());
            if ("BTC".equalsIgnoreCase(account.getCurrency())) {
                records = bitcoinService.getTransactions(balance.address(), fullScan);
            }
        }
        if (records == null || records.isEmpty()) {
            return 0;
        }
        importService.importRecords(records, account.getId(), userId);
        transactionService.saveImport(userId, account.getId(), records);
        return records.stream().filter(ImportDto::isSelected).count();
    }
}
