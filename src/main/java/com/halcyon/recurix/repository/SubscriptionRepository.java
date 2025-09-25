package com.halcyon.recurix.repository;

import com.halcyon.recurix.dto.CategorySpendingDto;
import com.halcyon.recurix.dto.ReminderDto;
import com.halcyon.recurix.model.Subscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {

    Flux<Subscription> findAllByUserId(Long userId, Pageable pageable);

    Mono<Integer> countByUserId(Long userId);

    Mono<Integer> countByUserIdAndPaymentDateBetween(Long userId, LocalDate startOfMonth, LocalDate endOfMonth);

    Flux<Subscription> findAllByPaymentDateBefore(LocalDate date);

    Mono<Subscription> findFirstByUserIdAndPaymentDateBetweenOrderByPriceDesc(Long userId, LocalDate startOfMonth, LocalDate endOfMonth);

    Mono<Subscription> findFirstByUserIdAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(Long userId, LocalDate fromDate);

    @Query("""
        SELECT s.id, s.user_id, s.name, s.price,
            s.payment_date, s.category, s.renewal_months, u.telegram_id
        FROM subscriptions s
        JOIN users u ON s.user_id = u.id
        JOIN user_settings us ON u.id = us.user_id
        WHERE us.reminders_enabled = true
        AND s.payment_date = CURRENT_DATE + us.reminder_days_before
    """)
    Flux<ReminderDto> findAllForReminding();

    @Query("""
        SELECT SUM(price) FROM subscriptions
        WHERE user_id = :userId
        AND payment_date BETWEEN :startOfMonth AND :endOfMonth
    """)
    Mono<BigDecimal> calculateMonthlyTotal(Long userId, LocalDate startOfMonth, LocalDate endOfMonth);

    @Query("""
        SELECT category, SUM(price) as total FROM subscriptions
        WHERE user_id = :userId
        AND payment_date BETWEEN :startOfMonth AND :endOfMonth
        GROUP BY category
        ORDER BY total DESC
    """)
    Flux<CategorySpendingDto> findSpendingByCategory(Long userId, LocalDate startOfMonth, LocalDate endOfMonth);
}
