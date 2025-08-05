package com.halcyon.recurix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "users")
public record User(
        @Id @Column("id") Long id,
        @Column("telegram_id") Long telegramId,
        @Column("first_name") String firstName,
        @Column("registered_at") Instant registeredAt
) {

    public User(Long telegramId, String firstName) {
        this(null, telegramId, firstName, Instant.now());
    }
}
