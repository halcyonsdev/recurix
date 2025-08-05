package com.halcyon.recurix.service;

import com.halcyon.recurix.callback.CallbackRouter;
import com.halcyon.recurix.command.BotCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateHandlerService {

    private final List<BotCommand> commands;
    private final ConversationService conversationService;
    private final CallbackRouter callbackRouter;

    public Mono<BotApiMethod<? extends Serializable>> handleUpdate(Update update) {
        return callbackRouter.handle(update)
                .switchIfEmpty(Mono.defer(() -> findAndExecuteCommand(update)))
                .switchIfEmpty(Mono.defer(() -> conversationService.handle(update)))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No handler or command found for update: {}", update);

                    if (update.hasMessage() && update.getMessage().hasText()) {
                        return Mono.just(new SendMessage(update.getMessage().getChatId().toString(), "Неизвестная команда."));
                    }

                    return Mono.empty();
                }));
    }

    private Mono<BotApiMethod<? extends Serializable>> findAndExecuteCommand(Update update) {
        return Flux.fromIterable(commands)
                .filter(command -> command.supports(update))
                .next()
                .flatMap(command -> command.execute(update));
    }
}
