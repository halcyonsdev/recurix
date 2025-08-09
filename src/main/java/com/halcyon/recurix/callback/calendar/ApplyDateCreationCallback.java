package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.support.SubscriptionContext;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Обрабатывает нажатие кнопки "Применить" в календаре во время создания новой подписки.
 * <p>
 * Этот обработчик является state-зависимым: он сработает, только если пользователь
 * находится в состоянии {@link ConversationState#AWAITING_SUBSCRIPTION_DATE}.
 * Его задача — сохранить выбранную дату и перевести диалог на финальный шаг подтверждения.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplyDateCreationCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_APPLY_PREFIX);
    }

    /**
     * Выполняет основную логику колбэка, если текущее состояние пользователя соответствует ожидаемому.
     * <p>
     * Метод асинхронно получает текущее состояние диалога. Если оно равно
     * {@link ConversationState#AWAITING_SUBSCRIPTION_DATE}, то управление передается
     * методу {@link #applyDateAndShowConfirmation(Update)}. В противном случае,
     * возвращается пустой {@code Mono}, что позволяет {@link com.halcyon.recurix.callback.CallbackRouter}
     * попробовать найти другой подходящий обработчик.
     *
     * @param update Входящий объект {@link Update} от Telegram, содержащий {@link CallbackQuery}.
     * @return {@code Mono}, содержащий {@link BotApiMethod} с ответным сообщением, если состояние
     *         пользователя было корректным. В противном случае, возвращает {@code Mono.empty()}.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        return stateService.getState(userId)
                .filter(ConversationState.AWAITING_SUBSCRIPTION_DATE::equals)
                .flatMap(state -> applyDateAndShowConfirmation(update))
                .switchIfEmpty(Mono.defer(Mono::empty));
    }

    /**
     * Применяет выбранную дату, обновляет контекст и состояние, а затем
     * отображает финальный экран подтверждения.
     *
     * @param update Входящий объект Update.
     * @return Mono с ответным сообщением {@link EditMessageText}.
     */
    private Mono<BotApiMethod<? extends Serializable>> applyDateAndShowConfirmation(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String dateStr = callbackQuery.getData().substring(CallbackData.CALENDAR_APPLY_PREFIX.length());
        LocalDate selectedDate = LocalDate.parse(dateStr);
        Long userId = callbackQuery.getFrom().getId();

        log.info("User {} applied date {} for a new subscription.", userId, selectedDate);

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    Subscription subscription = context.getSubscription();
                    subscription.setPaymentDate(selectedDate);
                    subscription.setCurrency("RUB");

                    return stateService.setContext(userId, context)
                            .thenReturn(stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_CONFIRMATION))
                            .thenReturn(subscription);
                })
                .map(subscription -> subscriptionMessageFactory.createEditMessage(
                        userId,
                        callbackQuery.getMessage().getMessageId(),
                        subscription,
                        keyboardService.getConfirmationKeyboard()
                ));
    }
}
