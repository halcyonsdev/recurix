package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.callback.subscription.edit.CancelEditCallback;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.command.ListCommand;
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

/**
 * Обрабатывает запросы на просмотр детальной информации о подписке.
 * <p>
 * Срабатывает при нажатии на конкретную подписку в списке. Загружает
 * актуальные данные из БД и отображает их пользователю вместе с клавиатурой
 * для дальнейших действий (редактирование, удаление).
 * <p>
 * Этот класс также используется другими обработчиками (например, {@link CancelEditCallback})
 * для возврата на экран детального просмотра.
 *
 * @see ListCommand
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionViewCallback implements Callback {

    private final SubscriptionService subscriptionService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory messageFactory;
    private final TelegramApiClient telegramApiClient;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_VIEW_PREFIX);
    }

    /**
     * Точка входа для стандартного запроса на просмотр.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return execute(update, CallbackData.SUB_VIEW_PREFIX);
    }

    /**
     * Перегруженная версия метода для вызова из других обработчиков.
     * <p>
     * Позволяет другим callback'ам (например, {@link CancelEditCallback}) повторно
     * использовать логику отображения, передавая свой уникальный префикс.
     *
     * @param update Объект {@link Update} от Telegram.
     * @param prefix Префикс, который нужно удалить из {@code callbackData} для парсинга.
     * @return {@code Mono} с {@link EditMessageText}.
     */
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update, String prefix) {
        CallbackQuery query = update.getCallbackQuery();

        return Mono.fromCallable(() -> parseCallbackContext(query.getData(), prefix))
                .flatMap(ctx -> {
                    log.info("User {} is viewing subscription {}. Page: {}. Callback: {}",
                            query.getFrom().getId(), ctx.subscriptionId(), ctx.pageNumber(), query.getData());

                    return subscriptionService.findById(ctx.subscriptionId)
                            .map(subscription -> EditMessageText.builder()
                                    .chatId(query.getMessage().getChatId())
                                    .messageId(query.getMessage().getMessageId())
                                    .text(messageFactory.formatSubscriptionDetail(subscription))
                                    .parseMode(ParseMode.MARKDOWN)
                                    .replyMarkup(keyboardService.getSubscriptionDetailKeyboard(ctx.subscriptionId, ctx.pageNumber))
                                    .build()
                            )
                            .switchIfEmpty(handleSubscriptionNotFound(query));
                });
    }

    /**
     * Извлекает ID подписки и номер страницы из строки callback-данных.
     *
     * @param callbackData Cтрока данных.
     * @param prefix       Префикс для удаления из строки.
     * @return Объект {@link ViewContext} с извлеченными данными.
     * @throws IllegalArgumentException если данные имеют неверный формат.
     */
    private ViewContext parseCallbackContext(String callbackData, String prefix) {
        String data = callbackData.substring(prefix.length());
        String[] parts = data.split("_");
        long subscriptionId = Long.parseLong(parts[0]);
        int pageNumber = Integer.parseInt(parts[1]);

        return new ViewContext(subscriptionId, pageNumber);
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
