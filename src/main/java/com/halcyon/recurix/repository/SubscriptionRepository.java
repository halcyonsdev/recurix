package com.halcyon.recurix.repository;

import com.halcyon.recurix.model.Subscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {

    Flux<Subscription> findAllByUserId(Long userId);
    Flux<Subscription> findAllByUserId(Long userId, Pageable pageable);

    Mono<Integer> countByUserId(Long userId);
}
