package com.halcyon.recurix.repository;

import com.halcyon.recurix.model.Subscription;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {

    Flux<Subscription> findAllByUserId(Long userId);
}
