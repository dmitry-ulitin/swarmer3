package com.swarmer.finance.models;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
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
@Entity(name = "transactions")
public class Transaction {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq")
	@SequenceGenerator(name = "transaction_seq", sequenceName = "transactions_id_seq", allocationSize = 1)
    Long id;
	@ManyToOne User owner;
    LocalDateTime opdate;
	@ManyToOne Account account;
    double debit;
	@ManyToOne Account recipient;
    double credit;
	@ManyToOne(cascade = {CascadeType.PERSIST,CascadeType.MERGE}) Category category;
    String currency;
    String party;
    String details;
    LocalDateTime created;
    LocalDateTime updated;
}
