package com.halcyon.recurix.repository;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.repository.mapper.Reminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {

    Flux<Subscription> findAllByUserId(Long userId, Pageable pageable);

    Mono<Integer> countByUserId(Long userId);

    @Query("""
        SELECT s.id, s.user_id, s.name, s.price, s.currency,
            s.payment_date, s.category, s.renewal_months, u.telegram_id
        FROM subscriptions s
        JOIN users u ON s.user_id = u.id
        JOIN user_settings us ON u.id = us.user_id
        WHERE us.reminders_enabled = true
        AND s.payment_date = CURRENT_DATE + us.reminder_days_before
    """)
    Flux<Reminder> findAllForReminding();

    Flux<Subscription> findAllByPaymentDateBefore(LocalDate date);
}
