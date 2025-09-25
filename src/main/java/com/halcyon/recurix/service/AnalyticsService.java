package com.halcyon.recurix.service;

import com.halcyon.recurix.dto.AnalyticsDto;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Собирает полную аналитическую сводку для пользователя за указанный месяц.
     *
     * @param user      Пользователь, для которого собирается статистика.
     * @param yearMonth Месяц и год, для которого собирается статистика.
     * @return {@code Mono} с {@link AnalyticsDto}, содержащим всю информацию.
     */
    public Mono<AnalyticsDto> getAnalyticsForMonth(RecurixUser user, YearMonth yearMonth) {
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        Mono<Integer> totalSubscriptionsMono = subscriptionRepository.countByUserIdAndPaymentDateBetween(user.id(), startOfMonth, endOfMonth)
                .defaultIfEmpty(0);
        Mono<BigDecimal> monthlyTotalMono = subscriptionRepository.calculateMonthlyTotal(user.id(), startOfMonth, endOfMonth)
                .defaultIfEmpty(BigDecimal.ZERO);
        Mono<AnalyticsDto.AnalyticsDtoBuilder> spendingByCategoryMono = subscriptionRepository.findSpendingByCategory(user.id(), startOfMonth, endOfMonth)
                .collectList()
                .map(list -> AnalyticsDto.builder().spendingByCategory(list));
        Mono<Optional<Subscription>> mostExpensiveMono = subscriptionRepository
                .findFirstByUserIdAndPaymentDateBetweenOrderByPriceDesc(user.id(), startOfMonth, endOfMonth)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
        Mono<Optional<Subscription>> nextPaymentMono = subscriptionRepository
                .findFirstByUserIdAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(user.id(), startOfMonth)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(
                totalSubscriptionsMono,
                monthlyTotalMono,
                spendingByCategoryMono,
                mostExpensiveMono,
                nextPaymentMono
        ).map(this::buildAnalyticsDtoFromTuple);
    }

    /**
     * Собирает финальный DTO из результатов параллельных запросов.
     *
     * @param tuple Кортеж с результатами всех Mono.
     * @return Финальный {@link AnalyticsDto}.
     */
    private AnalyticsDto buildAnalyticsDtoFromTuple(Tuple5<Integer, BigDecimal, AnalyticsDto.AnalyticsDtoBuilder, Optional<Subscription>, Optional<Subscription>> tuple) {
        return tuple.getT3()
                .totalSubscriptions(tuple.getT1())
                .monthlyTotal(tuple.getT2())
                .mostExpensive(tuple.getT4().orElse(null))
                .nextPayment(tuple.getT5().orElse(null))
                .build();
    }
}
