package com.swarmer.finance.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AclId {
    private Long userId;
    private Long groupId;
}
