package com.halcyon.recurix.service;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Mono<Subscription> save(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public Flux<Subscription> getAllByUserId(Long userId) {
        return subscriptionRepository.findAllByUserId(userId);
    }
}
