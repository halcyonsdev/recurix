package com.halcyon.recurix.config;

import com.halcyon.recurix.RecurixBot;
import com.halcyon.recurix.service.UpdateHandlerService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;

@Configuration
@AllArgsConstructor
public class SpringConfig {

    private final TelegramConfig telegramConfig;

    @Bean
    public SetWebhook setWebhook() {
        return SetWebhook.builder()
                .url(telegramConfig.getWebhookUrl())
                .build();
    }

    @Bean
    public RecurixBot recurixBot(SetWebhook setWebhook, UpdateHandlerService updateHandlerService) {
        RecurixBot recurixBot = new RecurixBot(
                setWebhook,
                telegramConfig.getBotToken(),
                updateHandlerService
        );

        recurixBot.setBotPath(telegramConfig.getWebhookUrl());
        recurixBot.setBotUsername(telegramConfig.getBotUsername());

        return recurixBot;
    }
}
