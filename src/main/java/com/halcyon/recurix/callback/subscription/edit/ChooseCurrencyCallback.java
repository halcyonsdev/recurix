package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.message.SubscriptionMessageFactory;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обрабатывает нажатие на кнопку выбора валюты в меню редактирования.
 * <p>
 * Срабатывает на callback-запросы, начинающиеся с префикса
 * {@link CallbackData#CURRENCY_SELECT_PREFIX}.
 * Обновляет валюту в контексте диалога и возвращает пользователя на экран
 * подтверждения данных подписки.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChooseCurrencyCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CURRENCY_SELECT_PREFIX);
    }

    /**
     * Обновляет валюту в контексте диалога и возвращает пользователя на экран подтверждения.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Извлекает код валюты из данных callback-запроса.</li>
     * <li>Загружает контекст диалога {@link SubscriptionContext} из Redis.</li>
     * <li>Устанавливает новую валюту в объект подписки и сохраняет контекст обратно в Redis.</li>
     * <li>Формирует и возвращает сообщение с обновленными данными и клавиатурой подтверждения.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText}
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = callbackQuery.getData();

        String currency = callbackData.substring(CallbackData.CURRENCY_SELECT_PREFIX.length());
        log.info("User {} selected currency: {}", userId, currency);

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    context.getSubscription().setCurrency(currency);
                    return stateService.setContext(userId, context).thenReturn(context);
                })
                .map(updatedContext -> {
                    Subscription subscription = updatedContext.getSubscription();
                    InlineKeyboardMarkup confirmationKeyboard = subscription.getId() == null
                            ? keyboardService.getConfirmationKeyboard()
                            : keyboardService.getEditConfirmationKeyboard(subscription.getId(), updatedContext.getPageNumber());

                    return subscriptionMessageFactory.createEditMessage(
                            userId,
                            messageId,
                            updatedContext.getSubscription(),
                            confirmationKeyboard);
                });
    }
}
