package com.halcyon.recurix.dto;

import com.halcyon.recurix.model.Subscription;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AnalyticsDto(
        Integer totalSubscriptions,
        BigDecimal monthlyTotal,
        List<CategorySpendingDto> spendingByCategory,
        Subscription mostExpensive,
        Subscription nextPayment
) {
}
