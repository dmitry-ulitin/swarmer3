package com.swarmer.finance.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity(name = "acl")
@IdClass(AclId.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acl {
    @Id @Column(name = "group_id") Long groupId;
    @ManyToOne @JoinColumn(name="group_id", insertable=false, updatable=false) AccountGroup group;
    @Id @Column(name = "user_id") Long userId;
    @ManyToOne @JoinColumn(name="user_id", insertable=false, updatable=false) User user;
    @Column(name = "is_admin") Boolean admin;
    @Column(name = "is_readonly") Boolean readonly;
    String name;
    Boolean deleted;
    LocalDateTime created;
    LocalDateTime updated;
}

