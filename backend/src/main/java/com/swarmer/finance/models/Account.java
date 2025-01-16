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
@Entity(name = "accounts")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_seq")
	@SequenceGenerator(name = "accounts_seq", sequenceName = "accounts_id_seq", allocationSize = 1)
    Long id;
    @ManyToOne AccountGroup group;
    String name;
    String currency;
    Double startBalance;
    Boolean deleted;
    LocalDateTime created;
    LocalDateTime updated;
}

