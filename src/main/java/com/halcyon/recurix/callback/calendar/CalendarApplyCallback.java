package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
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
import java.util.Set;

/**
 * Обрабатывает нажатие кнопки "Применить" в календаре во время редактирования существующей подписки.
 * <p>
 * Этот обработчик является state-зависимым и сработает, только если пользователь находится
 * в одном из состояний редактирования даты (например, {@link ConversationState#AWAITING_NEW_DATE}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarApplyCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    private static final Set<ConversationState> SUPPORTED_STATES = Set.of(
            ConversationState.AWAITING_SUBSCRIPTION_DATE,
            ConversationState.AWAITING_NEW_DATE
    );

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_APPLY_PREFIX);
    }

    /**
     * Выполняет основную логику, если текущее состояние пользователя соответствует ожидаемому.
     * <p>
     * Метод асинхронно проверяет состояние диалога. Если оно входит в список поддерживаемых
     * ({@link #SUPPORTED_STATES}), то управление передается методу {@link #applyDateAndShowConfirmation(Update)}.
     * В противном случае, возвращается {@code Mono.empty()}, что позволяет {@link com.halcyon.recurix.callback.CallbackRouter}
     * попробовать найти другой подходящий обработчик.
     *
     * @param update Входящий объект {@link Update} от Telegram.
     * @return {@code Mono}, содержащий {@link BotApiMethod} с ответным сообщением, если состояние
     *         было корректным, или {@code Mono.empty()} в противном случае.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        return stateService.getState(userId)
                .filter(SUPPORTED_STATES::contains)
                .flatMap(state -> applyDateAndShowConfirmation(update))
                .switchIfEmpty(Mono.defer(Mono::empty));
    }

    /**
     * Применяет выбранную дату, обновляет контекст, сбрасывает состояние и
     * отображает экран подтверждения с обновленными данными.
     *
     * @param update Входящий объект Update.
     * @return Mono с ответным сообщением {@link EditMessageText}.
     */
    private Mono<BotApiMethod<? extends Serializable>> applyDateAndShowConfirmation(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String dateStr = callbackQuery.getData().substring(CallbackData.CALENDAR_APPLY_PREFIX.length());
        LocalDate selectedDate = LocalDate.parse(dateStr);
        Long userId = callbackQuery.getFrom().getId();

        log.info("User {} applied date: {}", userId, selectedDate);

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    context.getSubscription().setPaymentDate(selectedDate);
                    return stateService.setContext(userId, context)
                            .thenReturn(stateService.clearState(userId))
                            .thenReturn(context);
                })
                .map(updatedContext -> subscriptionMessageFactory.createEditMessage(
                        userId,
                        callbackQuery.getMessage().getMessageId(),
                        updatedContext.getSubscription(),
                        keyboardService.getConfirmationKeyboard()
                ));
    }
}
