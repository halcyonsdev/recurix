package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.model.Subscription;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает выбор предопределенного периода списания (ежемесячно/ежегодно).
 * <p>
 * Срабатывает при нажатии на кнопки "Ежемесячно" или "Ежегодно".
 * Обновляет период в контексте диалога и возвращает пользователя
 * на соответствующий экран подтверждения.
 *
 * @see CustomPeriodCallback
 * @see EditPeriodCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChoosePeriodCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory messageFactory;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.PERIOD_SELECT_PREFIX);
    }

    /**
     * Запускает процесс обновления периода и возврата к экрану подтверждения.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText}.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackContext context = parseCallbackContext(update.getCallbackQuery());

        log.info("User {} selected renewal period: {} months", context.userId, context.months);

        return stateService.getContext(context.userId, SubscriptionContext.class)
                .flatMap(subscriptionContext -> updateContextAndProceed(context, subscriptionContext))
                .map(finalContext -> createConfirmationMessage(update.getCallbackQuery(), finalContext));
    }

    /**
     * Извлекает ID пользователя и количество месяцев из данных запроса.
     *
     * @param query Исходный {@link CallbackQuery}.
     * @return Объект {@link CallbackContext} с данными.
     */
    private CallbackContext parseCallbackContext(CallbackQuery query) {
        Long userId = query.getFrom().getId();
        String monthsStr = query.getData().substring(CallbackData.PERIOD_SELECT_PREFIX.length());
        Integer months = Integer.parseInt(monthsStr);

        return new CallbackContext(userId, months);
    }

    /**
     * Обновляет поле `renewalMonths` в контексте и сохраняет его в Redis.
     *
     * @param callbackContext     Данные, извлеченные из запроса.
     * @param subscriptionContext Контекст диалога из Redis.
     * @return {@code Mono} с обновленным контекстом диалога.
     */
    private Mono<SubscriptionContext> updateContextAndProceed(CallbackContext callbackContext,
                                                              SubscriptionContext subscriptionContext) {
        subscriptionContext.getSubscription().setRenewalMonths(callbackContext.months);
        return stateService.setContext(callbackContext.userId, subscriptionContext).thenReturn(subscriptionContext);
    }

    /**
     * Создает финальное сообщение с подтверждением на основе обновленного контекста.
     *
     * @param query   Исходный {@link CallbackQuery}.
     * @param context Финальный, обновленный контекст диалога.
     * @return Готовый объект {@link EditMessageText}.
     */
    private EditMessageText createConfirmationMessage(CallbackQuery query, SubscriptionContext context) {
        Subscription subscription = context.getSubscription();

        InlineKeyboardMarkup keyboardMarkup = (subscription.getId() == null)
                ? keyboardService.getConfirmationKeyboard()
                : keyboardService.getEditConfirmationKeyboard(subscription.getId(), context.getPageNumber());

        return messageFactory.createEditMessage(
                query.getFrom().getId(),
                query.getMessage().getMessageId(),
                subscription,
                keyboardMarkup);
    }

    /**
     * Внутренний record для хранения данных из callback-запроса.
     */
    private record CallbackContext(Long userId, Integer months) {}
}
