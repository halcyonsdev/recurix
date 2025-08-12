package com.halcyon.recurix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationStateService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration STATE_TTL = Duration.ofHours(1);

    public Mono<Void> setState(Long userId, ConversationState state) {
        return redisTemplate.opsForValue()
                .set(stateKey(userId), state, STATE_TTL)
                .then();
    }

    private String stateKey(Long userId) {
        return "state:" + userId;
    }

    public Mono<ConversationState> getState(Long userId) {
        return fetch(stateKey(userId), ConversationState.class);
    }

    private <T> Mono<T> fetch(String key, Class<T> targetClass) {
        return redisTemplate.opsForValue()
                .get(key)
                .doOnNext(value -> log.info("Fetched value = {} for key = {}", value, key))
                .map(value -> objectMapper.convertValue(value, targetClass))
                .doOnSuccess(value -> {
                    if (value == null) {
                        log.info("No cached value found for key = {}", key);
                    }
                });
    }

    public <T> Mono<T> getContext(Long userId, Class<T> type) {
        return fetch(contextKey(userId), type);
    }

    public Mono<Void> setContext(Long userId, Object context) {
        return redisTemplate.opsForValue()
                .set(contextKey(userId), context, STATE_TTL)
                .then();
    }

    private String contextKey(Long userId) {
        return "context:" + userId;
    }

    public Mono<Void> clearState(Long userId) {
        return redisTemplate.delete(stateKey(userId)).then();
    }

    public Mono<Void> clearContext(Long userId) {
        return redisTemplate.delete(contextKey(userId)).then();
    }

    public Mono<Void> endConversation(Long userId) {
        return clearState(userId)
                .then(clearContext(userId))
                .then(clearListContext(userId));
    }

    public Mono<Void> setListContext(Long userId, SubscriptionListContext context) {
        return redisTemplate.opsForValue()
                .set(listContextKey(userId), context, STATE_TTL)
                .then();
    }

    private String listContextKey(Long userId) {
        return "list_context:" + userId;
    }

    public Mono<SubscriptionListContext> getListContext(Long userId) {
        return fetch(listContextKey(userId), SubscriptionListContext.class);
    }

    public Mono<Void> clearListContext(Long userId) {
        return redisTemplate.delete(listContextKey(userId)).then();
    }
}
