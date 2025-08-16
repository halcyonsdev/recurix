package com.halcyon.recurix.callback.subscription.add;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обработчик callback-запроса для перезапуска диалога добавления подписки.
 * <p>
 * Срабатывает, когда пользователь нажимает кнопку "Начать заново" на экране
 * подтверждения данных. Полностью сбрасывает ранее введенные данные и
 * возвращает пользователя к первому шагу диалога.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestartSubscriptionCallback implements Callback {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final TelegramApiClient telegramApiClient;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.SUBSCRIPTION_RESTART.equals(callbackData);
    }

    /**
     * Выполняет полный сброс диалога и возвращает пользователя к первому шагу.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Отправляет пользователю всплывающее уведомление о перезапуске.</li>
     * <li>Инициирует новый диалог добавления, аналогично
     * {@link com.halcyon.recurix.callback.main.AddMenuCallback}.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} is restarting the subscription creation process.", userId);

        return telegramApiClient.sendAnswerCallbackQuery(
                callbackQuery.getId(),
                messageService.getMessage("dialog.confirm.restarted")).then(startCreateSubscriptionDialog(userId, messageId));
    }

    /**
     * Инициализирует и начинает диалог добавления подписки.
     *
     * @param userId    ID пользователя.
     * @param messageId ID сообщения для редактирования.
     * @return {@code Mono} с первым сообщением диалога.
     */
    private Mono<BotApiMethod<? extends Serializable>> startCreateSubscriptionDialog(Long userId, Integer messageId) {
        return stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_NAME)
                .then(Mono.fromCallable(() -> EditMessageText.builder()
                        .chatId(userId)
                        .messageId(messageId)
                        .text(messageService.getMessage("dialog.add.prompt.name"))
                        .replyMarkup(keyboardService.getBackToMenuKeyboard())
                        .build()));
    }
}
