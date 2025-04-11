package com.swarmer.finance.services;

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.swarmer.finance.models.AccountAddress;
import com.swarmer.finance.repositories.AccountAddressRepository;
import com.swarmer.finance.repositories.AccountGroupRepository;
import com.swarmer.finance.repositories.AclRepository;

import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final AccountGroupRepository groupRepository;
    private final AclRepository aclRepository;
    private final AccountAddressRepository addressRepository;
    private final TronService tronService;
    private final ImportService importService;
    private final TransactionService transactionService;

    public WalletService(AccountGroupRepository groupRepository, AclRepository aclRepository,
            AccountAddressRepository addressRepository, TronService tronService, ImportService importService,
            TransactionService transactionService) {
        this.groupRepository = groupRepository;
        this.aclRepository = aclRepository;
        this.addressRepository = addressRepository;
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
        var accList = Stream.concat(userGroups.stream(), sharedGroups.stream())
                .flatMap(group -> group.getAccounts().stream())
                .map(account -> account.getId())
                .filter(id -> accountIdsFilter == null || accountIdsFilter.isEmpty() || accountIdsFilter.contains(id))
                .toList();
        var addresses = addressRepository.findByAccountIdIn(accList);
        for (var address : addresses) {
            importAddress(address, userId);
        }
    }

    @Transactional
    public void importAddress(AccountAddress address, Long userId) {
        if ("trc20".equals(address.getChain())) {
            var balance = tronService.getWalletBalance(address.getAddress());
            if ("TRX".equalsIgnoreCase(address.getAccount().getCurrency())) {
                var records = tronService.getTrxTransactions(balance.hexAddress());
                importService.importRecords(records, address.getAccountId(), userId);
                transactionService.saveImport(userId, address.getAccountId(), records);
            } else {
                var records = tronService.getContractTransactions(balance.hexAddress()).stream()
                        .filter(r -> r.getCurrency().equals(address.getAccount().getCurrency())).toList();
                importService.importRecords(records, address.getAccountId(), userId);
                transactionService.saveImport(userId, address.getAccountId(), records);
            }
        }
    }
}
