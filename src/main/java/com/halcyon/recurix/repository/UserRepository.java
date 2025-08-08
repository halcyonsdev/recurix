package com.halcyon.recurix.repository;

import com.halcyon.recurix.model.RecurixUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<RecurixUser, Long> {

    Mono<RecurixUser> findByTelegramId(Long telegramId);
}
