package com.halcyon.recurix.command;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface BotCommand {

    boolean supports(Update update);

    Mono<BotApiMethod<? extends Serializable>> execute(Update update);
}
