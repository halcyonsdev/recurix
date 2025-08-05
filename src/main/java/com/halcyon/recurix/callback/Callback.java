package com.halcyon.recurix.callback;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface Callback {

    boolean supports(String callbackData);

    Mono<BotApiMethod<? extends Serializable>> execute(Update update);
}
