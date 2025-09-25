package com.halcyon.recurix.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReminderDto(
        Long id,
        Long userId,
        String name,
        BigDecimal price,
        LocalDate paymentDate,
        String category,
        Integer renewalMonths,
        Long telegramId
) {}
