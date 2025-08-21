package com.halcyon.recurix.repository;

import com.halcyon.recurix.model.UserSettings;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserSettingsRepository extends ReactiveCrudRepository<UserSettings, Long> {

    Mono<UserSettings> findByUserId(Long userId);
}
