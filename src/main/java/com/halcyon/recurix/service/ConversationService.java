package com.halcyon.recurix.service;

import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.handler.ConversationStepHandler;
import java.io.Serializable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationStateService stateService;
    private final List<ConversationStepHandler> stepHandlers;

    public Mono<BotApiMethod<? extends Serializable>> handle(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return Mono.empty();
        }

        return stateService.getState(update.getMessage().getFrom().getId())
                .flatMap(state -> findAndExecuteHandler(state, update));
    }

    private Mono<BotApiMethod<? extends Serializable>> findAndExecuteHandler(ConversationState state, Update update) {
        log.debug("Finding handler for state: {}", state);

        return Flux.fromIterable(stepHandlers)
                .filter(handler -> handler.supports(state))
                .next()
                .flatMap(handler -> handler.execute(update));
    }
}
