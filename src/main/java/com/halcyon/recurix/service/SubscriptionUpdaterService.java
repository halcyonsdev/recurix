package com.halcyon.recurix.service;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;

/**
 * Сервис для автоматического обновления дат прошедших подписок.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUpdaterService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Запускается ежедневно в 15:00 по МСК для обновления дат "просроченных" подписок.
     */
    @Scheduled(cron = "0 0 15 * * *", zone = "Europe/Moscow")
    public void updatePastSubscriptions() {
        log.info("SCHEDULER: Starting daily subscription update task...");

        subscriptionRepository.findAllByPaymentDateBefore(LocalDate.now())
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::updateSingleSubscriptionDate)
                .sequential()
                .count()
                .doOnSuccess(count -> log.info("SCHEDULER: Update task finished. Updated {} subscriptions.", count))
                .doOnError(e -> log.error("SCHEDULER: Error during subscription update task.", e))
                .subscribe();
    }

    /**
     * Обновляет дату следующего платежа у одной подписки.
     *
     * @param subscription Подписка для обновления.
     * @return {@code Mono<Void>}
     */
    private Mono<Void> updateSingleSubscriptionDate(Subscription subscription) {
        LocalDate oldDate = subscription.getPaymentDate();
        LocalDate newDate = oldDate;

        while (!newDate.isAfter(oldDate)) {
            newDate = newDate.plusMonths(subscription.getRenewalMonths());
        }

        subscription.setPaymentDate(newDate);
        log.info("Updating payment date for subscription {}: from {} to {}", subscription.getId(), oldDate, newDate);
        return subscriptionRepository.save(subscription).then();
    }
}
