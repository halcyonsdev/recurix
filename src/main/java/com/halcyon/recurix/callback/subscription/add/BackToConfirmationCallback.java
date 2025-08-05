package com.halcyon.recurix.callback.subscription.add;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.support.SubscriptionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для возврата из меню редактирования на экран подтверждения.
 * <p>
 * Срабатывает, когда пользователь нажимает кнопку "Назад" в меню, где
 * предлагается изменить отдельные поля подписки (название, цену и т.д.).
 */
@Component
@RequiredArgsConstructor
public class BackToConfirmationCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.BACK_TO_CONFIRMATION.equals(callbackData);
    }

    /**
     * Возвращает пользователя на экран подтверждения данных создаваемой подписки.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     *     <li>Загружает из Redis актуальный контекст {@link SubscriptionContext} диалога.</li>
     *     <li>Использует {@link SubscriptionMessageFactory} для перерисовки сообщения с данными подписки и кнопками "Сохранить", "Изменить" и т.д.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления исходного сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        return stateService.getContext(userId, SubscriptionContext.class)
                .map(context -> subscriptionMessageFactory.createEditMessage(
                        userId,
                        messageId,
                        context.getSubscription(),
                        keyboardService.getConfirmationKeyboard()
                ));
    }
}
