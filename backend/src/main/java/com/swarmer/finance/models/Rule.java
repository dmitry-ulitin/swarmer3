package com.swarmer.finance.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(name = "condition_type", nullable = false)
    private ConditionType conditionType;
    
    @Column(name = "condition_value", nullable = false)
    private String conditionValue;
    
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime created = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updated = LocalDateTime.now();
} 