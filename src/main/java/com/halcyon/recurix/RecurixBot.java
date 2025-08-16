package com.halcyon.recurix;

import com.halcyon.recurix.service.UpdateHandlerService;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import reactor.core.publisher.Mono;

@Getter
@Setter
public class RecurixBot extends SpringWebhookBot {

    private String botPath;
    private String botUsername;

    private final UpdateHandlerService updateHandlerService;

    public RecurixBot(SetWebhook setWebhook, String botToken, UpdateHandlerService updateHandlerService) {
        super(setWebhook, botToken);
        this.updateHandlerService = updateHandlerService;
    }

    public Mono<BotApiMethod<? extends Serializable>> onUpdateReceived(Update update) {
        return updateHandlerService.handleUpdate(update);
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return null;
    }
}
