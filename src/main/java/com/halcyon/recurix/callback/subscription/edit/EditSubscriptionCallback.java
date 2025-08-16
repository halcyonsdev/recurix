package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
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
 * Обработчик callback-запроса для перехода в меню редактирования подписки.
 * <p>
 * Срабатывает, когда пользователь нажимает кнопку "Изменить" на экране
 * подтверждения данных создаваемой подписки.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EditSubscriptionCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.SUBSCRIPTION_EDIT.equals(callbackData);
    }

    /**
     * Отображает пользователю меню с кнопками для редактирования отдельных полей подписки.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Загружает из Redis актуальный контекст {@link SubscriptionContext}.</li>
     * <li>Использует {@link SubscriptionMessageFactory} для формирования сообщения со списком полей для
     * редактирования.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления исходного сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} entered the subscription edit menu.", userId);

        return stateService.getContext(userId, SubscriptionContext.class)
                .map(context -> subscriptionMessageFactory.createEditMessage(
                        userId,
                        messageId,
                        context.getSubscription(),
                        keyboardService.getEditKeyboard(CallbackData.BACK_TO_CONFIRMATION)));
    }
}
