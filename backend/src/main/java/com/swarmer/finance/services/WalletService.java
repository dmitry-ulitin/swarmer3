package com.swarmer.finance.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        var wallets = Stream.concat(userGroups.stream(), sharedGroups.stream())
                .flatMap(group -> group.getAccounts().stream())
                .filter(account -> !account.isDeleted())
                .filter(account -> account.getChain() != null && !account.getChain().isBlank())
                .filter(account -> account.getAddress() != null && !account.getAddress().isBlank()).toList();
        var filtered = wallets.stream()
                .filter(account -> (accountIdsFilter == null || accountIdsFilter.isEmpty()
                        || accountIdsFilter.contains(account.getId())) && account.getChain() != null)
                .toList();
        var balances = transactionService.getBalances(filtered.stream().map(Account::getId).toList(), null, null, null);

        for (var account : filtered) {
            var debit = balances.stream().filter(b -> account.getId().equals(b.accountId()))
                    .map(b -> b.debit())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var credit = balances.stream().filter(b -> account.getId().equals(b.recipientId()))
                    .map(b -> b.credit())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var balance = account.getStartBalance().add(credit).subtract(debit).setScale(account.getScale(),
                    RoundingMode.HALF_DOWN);
            var records = importAccount(account, balance, userId, fullScan);
            if (records == null || records.isEmpty()) {
                continue;
            }
            importService.importWaletRecords(records, account, wallets.stream().map(Account::getId).toList(), userId);
            transactionService.saveImport(userId, account.getId(), records);
            count += records.stream().filter(ImportDto::isSelected).count();
        }
        return count;
    }

    private List<ImportDto> importAccount(Account account, BigDecimal balance, Long userId, boolean fullScan) {
        List<ImportDto> records = null;
        if ("trc20".equals(account.getChain()) && account.getAddress() != null) {
            var wallet = tronService.getWalletBalance(account.getAddress());
            if (!wallet.trxBalance().equals(balance) && "TRX".equalsIgnoreCase(account.getCurrency())) {
                records = tronService.getTrxTransactions(wallet.hexAddress(), fullScan);
            } else if (!wallet.usdtBalance().equals(balance) && "USDT".equalsIgnoreCase(account.getCurrency())) {
                records = tronService.getContractTransactions(wallet.address(), fullScan).stream()
                        .filter(r -> r.getCurrency().equals(account.getCurrency())).toList();
            }
        } else if ("btc".equals(account.getChain()) && "BTC".equalsIgnoreCase(account.getCurrency())
                && account.getAddress() != null) {
            var wallet = bitcoinService.getWalletBalance(account.getAddress());
            if (!wallet.btcBalance().equals(balance)) {
                records = bitcoinService.getTransactions(wallet.address(), fullScan);
            }
        }
        return records;
    }
}
