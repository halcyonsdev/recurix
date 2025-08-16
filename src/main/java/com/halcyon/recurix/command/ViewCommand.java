package com.halcyon.recurix.command;

import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.support.PayloadEncoder;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCommand implements BotCommand {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final PayloadEncoder payloadEncoder;
    private final TelegramApiClient telegramApiClient;

    private static final Pattern VIEW_COMMAND_PATTERN = Pattern.compile("^/view_([a-zA-Z0-9_-]+)$");

    @Override
    public boolean supports(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return false;
        }

        System.out.println(update.getMessage().getText());
        return VIEW_COMMAND_PATTERN.matcher(update.getMessage().getText()).matches();
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        String commandText = update.getMessage().getText();
        Matcher matcher = VIEW_COMMAND_PATTERN.matcher(commandText);
        Long chatId = update.getMessage().getChatId();
        Integer commandMessageId = update.getMessage().getMessageId();

        if (!matcher.matches()) {
            log.warn("ViewCommand executed but pattern did not match inside execute method: {}", commandText);
            return Mono.empty();
        }

        try {
            String encodedPayload = matcher.group(1);
            PayloadEncoder.Payload payload = payloadEncoder.decode(encodedPayload);

            log.info(
                    "User {} executed /view command for subscription ID {} from page {}.",
                    update.getMessage().getFrom().getId(),
                    payload.subscriptionId(),
                    payload.pageNumber());

            return Mono.when(
                    telegramApiClient.deleteMessage(chatId, payload.messageId()),
                    telegramApiClient.deleteMessage(chatId, commandMessageId))
                    .then(subscriptionService.findById(payload.subscriptionId())
                            .map(subscription -> SendMessage.builder()
                                    .chatId(chatId)
                                    .text(subscriptionMessageFactory.formatSubscriptionDetail(subscription))
                                    .parseMode(ParseMode.MARKDOWN)
                                    .replyMarkup(keyboardService.getSubscriptionDetailKeyboard(payload.subscriptionId(),
                                            payload.pageNumber()))
                                    .build())
                            .switchIfEmpty(Mono.fromCallable(() -> new SendMessage(chatId.toString(),
                                    messageService.getMessage("subscription.not_found"))))
                            .map(sendMessage -> sendMessage));
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode view command payload: {}", commandText, e);
            return Mono.just(new SendMessage(chatId.toString(), "Некорректная или устаревшая команда."));
        }
    }
}
