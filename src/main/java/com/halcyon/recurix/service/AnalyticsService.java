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

        Mono<Integer> totalSubscriptionsMono = subscriptionRepository.countByUserId(user.id())
                .defaultIfEmpty(0);
        Mono<BigDecimal> monthlyTotalMono = subscriptionRepository.calculateMonthlyTotal(user.id(), startOfMonth, endOfMonth)
                .defaultIfEmpty(BigDecimal.ZERO);
        Mono<AnalyticsDto.AnalyticsDtoBuilder> spendingByCategoryMono = subscriptionRepository.findSpendingByCategory(user.id(), startOfMonth, endOfMonth)
                .collectList()
                .map(list -> AnalyticsDto.builder().spendingByCategory(list));
        Mono<Subscription> mostExpensiveMono = subscriptionRepository.findFirstByUserIdAndPaymentDateBetweenOrderByPriceDesc(user.id(), startOfMonth, endOfMonth);
        Mono<Subscription> nextPaymentMono = subscriptionRepository.findFirstByUserIdAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(user.id(), LocalDate.now());

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
    private AnalyticsDto buildAnalyticsDtoFromTuple(Tuple5<Integer, BigDecimal, AnalyticsDto.AnalyticsDtoBuilder, Subscription, Subscription> tuple) {
        return tuple.getT3()
                .totalSubscriptions(tuple.getT1())
                .monthlyTotal(tuple.getT2())
                .mostExpensive(tuple.getT4())
                .nextPayment(tuple.getT5())
                .build();
    }
}
