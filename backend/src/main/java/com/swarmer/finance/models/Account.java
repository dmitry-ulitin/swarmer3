package com.swarmer.finance.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private AccountGroup group;

    @Column(nullable = true)
    private String name;

    @Column(nullable = false, length = 5)
    private String currency;

    @Column(name = "start_balance", nullable = false)
    private BigDecimal startBalance = BigDecimal.ZERO;
    
    @Column
    private String chain;
    
    @Column
    private String address;

    @Column(nullable = false)
    private Integer scale = 2;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updated = LocalDateTime.now();
} 