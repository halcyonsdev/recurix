package com.halcyon.recurix.dto;

import java.math.BigDecimal;

public record CategorySpendingDto(
        String category,
        BigDecimal total
) {
}
