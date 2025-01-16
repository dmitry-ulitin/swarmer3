package com.swarmer.finance.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "rules")
public class Rule {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_seq")
	@SequenceGenerator(name = "rule_seq", sequenceName = "rules_id_seq", allocationSize = 1)
    Long id;   
	Long ownerId;
    ConditionType conditionType;
    String conditionValue;
    @ManyToOne Category category;
    LocalDateTime created;
    LocalDateTime updated;
}
