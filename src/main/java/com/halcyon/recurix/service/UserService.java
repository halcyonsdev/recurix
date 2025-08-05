package com.halcyon.recurix.service;

import com.halcyon.recurix.model.User;
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

    public Mono<User> findOrCreateUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return userRepository.findByTelegramId(telegramUser.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("User with telegramId={} not found. Creating new user.", telegramUser.getId());

                    User user = new User(telegramUser.getId(), telegramUser.getFirstName());
                    return userRepository.save(user);
                }));
    }
}
