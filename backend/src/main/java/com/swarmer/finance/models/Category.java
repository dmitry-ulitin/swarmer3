package com.swarmer.finance.models;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Entity(name = "categories")
public class Category {
    @Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq")
	@SequenceGenerator(name = "category_seq", sequenceName = "categories_id_seq", allocationSize = 1)
	Long id;
	Long ownerId;
	Long parentId;
	@JsonIgnore	@ManyToOne
	@JoinColumn(name="parentId", insertable=false, updatable=false)
	Category parent;
	String name;
	@JsonIgnore	LocalDateTime created;
	@JsonIgnore	LocalDateTime updated;

	public TransactionType getType() {
		var root = this;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		final var rootId = root.getId();
		return Stream.of(TransactionType.values()).filter(c -> rootId == c.getValue()).findFirst().orElseThrow(IllegalArgumentException::new);
	}

	public Long getLevel() {
		return parent == null ? 0 : (1 + parent.getLevel());
	}

	public String getFullName() {
		return parent == null || parent.getId() < 4 ? name : parent.getFullName() + " / " + name;
	}
}
