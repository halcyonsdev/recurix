package com.halcyon.recurix.handler;

import java.io.Serializable;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

public interface ConversationStepHandler {

    boolean supports(ConversationState state);

    Mono<BotApiMethod<? extends Serializable>> execute(Update update);
}
