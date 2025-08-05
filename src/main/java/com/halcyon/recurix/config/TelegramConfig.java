package com.halcyon.recurix.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class TelegramConfig {

    @Value("${telegram.bot.webhook-url}")
    private String webhookUrl;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;
}
