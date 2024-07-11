package com.swarmer.finance.models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "account_groups")
public class AccountGroup {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_groups_seq")
	@SequenceGenerator(name = "account_groups_seq", sequenceName = "account_groups_id_seq", allocationSize = 1)
	Long id;
	@ManyToOne User owner;
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL) List<Acl> acls;
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL) List<Account> accounts;
	String name;
    Boolean deleted;
	LocalDateTime created;
	LocalDateTime updated;
}
