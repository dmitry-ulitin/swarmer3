package com.swarmer.finance.dto.dump;

import java.time.LocalDateTime;
import java.util.List;

import com.swarmer.finance.models.AccountGroup;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.Rule;
import com.swarmer.finance.models.Transaction;

public record Dump(Long ownerId, LocalDateTime created, List<DumpGroup> groups, List<DumpCategory> categories,
                List<DumpTransaction> transactions, List<DumpRule> rules) {

        public static Dump fromEntities(Long userId, List<AccountGroup> groups, List<Category> categories,
                        List<Transaction> transactions, List<Rule> rules) {
                return new Dump(userId, LocalDateTime.now(),
                                groups.stream().map(DumpGroup::fromEntity).toList(),
                                categories.stream().map(DumpCategory::fromEntity).toList(),
                                transactions.stream().map(DumpTransaction::fromEntity).toList(),
                                rules.stream().map(DumpRule::fromEntity).toList());
        }
}
