package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionViewCallback implements Callback {

    private final SubscriptionService subscriptionService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;
    private final TelegramApiClient telegramApiClient;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_VIEW_PREFIX);
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData().substring(CallbackData.SUB_VIEW_PREFIX.length());

        return Mono.fromCallable(() -> {
            String[] parts = data.split("_");
            Long subscriptionId = Long.parseLong(parts[0]);
            int pageNumber = Integer.parseInt(parts[1]);
            return new ViewContext(subscriptionId, pageNumber);
        }).flatMap(ctx -> subscriptionService.findById(ctx.subscriptionId)
                .map(subscription -> EditMessageText.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .text(subscriptionMessageFactory.formatSubscriptionDetail(subscription))
                        .parseMode(ParseMode.MARKDOWN)
                        .replyMarkup(keyboardService.getSubscriptionDetailKeyboard(ctx.subscriptionId, ctx.pageNumber))
                        .build()
                )
                .switchIfEmpty(handleSubscriptionNotFound(callbackQuery))
        );
    }

    /**
     * Обрабатывает случай, когда подписка не найдена в базе данных.
     * Отправляет пользователю всплывающее уведомление и возвращает пустой Mono.
     *
     * @param query Входящий CallbackQuery.
     * @return {@code Mono.empty()} с правильным типом.
     */
    private Mono<EditMessageText> handleSubscriptionNotFound(CallbackQuery query) {
        log.warn("User {} tried to view a non-existent subscription.", query.getFrom().getId());

        return telegramApiClient.sendAnswerCallbackQuery(
                        query.getId(),
                        messageService.getMessage("subscription.not_found"),
                        true
                )
                .then(Mono.empty());
    }

    private record ViewContext(Long subscriptionId, int pageNumber) {}
}
