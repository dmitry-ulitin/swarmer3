package com.swarmer.finance.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Dump(
                @JsonProperty("owner_id") Long ownerId,
                LocalDateTime created,
                List<DumpGroup> groups,
                List<DumpCategory> categories,
                List<DumpTransaction> transactions,
                List<DumpRule> rules) {
        public static Dump mapUsers(Dump dump, Map<Long, Long> userIds) {
                return new Dump(
                                userIds.get(dump.ownerId()),
                                dump.created(),
                                dump.groups().stream().map(group -> DumpGroup.mapUsers(group, userIds)).toList(),
                                dump.categories(),
                                dump.transactions(),
                                dump.rules());
        }
}
