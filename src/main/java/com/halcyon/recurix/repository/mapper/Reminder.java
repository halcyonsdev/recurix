package com.halcyon.recurix.repository.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reminder(
        Long id,
        Long userId,
        String name,
        BigDecimal price,
        String currency,
        LocalDate paymentDate,
        String category,
        Integer renewalMonths,
        Long telegramId
) {}
