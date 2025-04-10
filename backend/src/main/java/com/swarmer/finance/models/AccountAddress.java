package com.swarmer.finance.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountAddress {
    @Id
    private Long account_id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String chain;

    @Column(nullable = false)
    private String address;
} 