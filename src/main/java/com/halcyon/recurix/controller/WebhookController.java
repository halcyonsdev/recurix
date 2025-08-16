package com.halcyon.recurix.controller;

import com.halcyon.recurix.RecurixBot;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final RecurixBot recurixBot;

    @PostMapping("/")
    public Mono<BotApiMethod<? extends Serializable>> onUpdateReceived(@RequestBody Update update) {
        return recurixBot.onUpdateReceived(update);
    }
}
