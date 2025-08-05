package com.halcyon.recurix.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name = "subscriptions")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subscription {

    @Id
    @Column("id")
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("name")
    private String name;

    @Column("price")
    private BigDecimal price;

    @Column("currency")
    private String currency;

    @Column("payment_date")
    private LocalDate paymentDate;

    @Column("category")
    private String category;
}

