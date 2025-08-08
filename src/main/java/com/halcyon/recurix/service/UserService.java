package com.halcyon.recurix.service;

import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Mono<RecurixUser> findOrCreateUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return userRepository.findByTelegramId(telegramUser.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("User with telegramId={} not found. Creating new user.", telegramUser.getId());

                    RecurixUser user = new RecurixUser(telegramUser.getId(), telegramUser.getFirstName());
                    return userRepository.save(user);
                }));
    }
}
