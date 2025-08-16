package com.halcyon.recurix.callback;

import java.io.Serializable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackRouter {

    private final List<Callback> callbacks;

    public Mono<BotApiMethod<? extends Serializable>> handle(Update update) {
        if (!update.hasCallbackQuery()) {
            return Mono.empty();
        }

        String callbackData = update.getCallbackQuery().getData();
        log.info("Routing callback query with data: {}", callbackData);

        return Flux.fromIterable(callbacks)
                .filter(handler -> handler.supports(callbackData))
                .next()
                .flatMap(handler -> handler.execute(update));
    }
}
