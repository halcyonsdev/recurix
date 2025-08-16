package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
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
import java.time.LocalDate;
import java.util.Set;

/**
 * Обрабатывает нажатие кнопки "Применить" в календаре.
 * <p>
 * Этот обработчик является state-зависимым и универсальным. Он срабатывает
 * как при создании новой подписки (состояние {@code AWAITING_SUBSCRIPTION_DATE}),
 * так и при редактировании даты существующей ({@code AWAITING_NEW_DATE}).
 * <p>
 * В зависимости от контекста (новая или существующая подписка), он переводит
 * диалог на соответствующий следующий шаг.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarApplyCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory messageFactory;

    private static final Set<ConversationState> SUPPORTED_STATES = Set.of(
            ConversationState.AWAITING_SUBSCRIPTION_DATE,
            ConversationState.AWAITING_NEW_DATE
    );

    @Override
    public boolean supports(String callbackData) {
        System.out.println(callbackData);
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_APPLY_PREFIX);
    }

    /**
     * Проверяет состояние пользователя и вызывает соответствующую логику.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} или {@code Mono.empty()}.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        return stateService.getState(userId)
                .filter(SUPPORTED_STATES::contains)
                .flatMap(state -> applyDateAndProceed(update))
                .switchIfEmpty(Mono.defer(Mono::empty));
    }

    /**
     * Применяет выбранную дату, обновляет контекст и состояние, а затем
     * отображает финальный экран подтверждения.
     *
     * @param update Входящий объект Update.
     * @return Mono с ответным сообщением {@link EditMessageText}.
     */
    private Mono<BotApiMethod<? extends Serializable>> applyDateAndProceed(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        LocalDate selectedDate = LocalDate.parse(query.getData().substring(CallbackData.CALENDAR_APPLY_PREFIX.length()));
        Long userId = query.getFrom().getId();

        log.info("User {} applied date {}.", userId, selectedDate);

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> updateContextWithDate(userId, context, selectedDate))
                .map(finalContext -> createConfirmationMessage(query, finalContext));
    }

    /**
     * Обновляет контекст подписки, устанавливая новую дату и изменяя состояние диалога.
     *
     * @param userId       ID пользователя.
     * @param context      Текущий контекст диалога.
     * @param selectedDate Выбранная пользователем дата.
     * @return {@code Mono}, содержащий обновленный контекст.
     */
    private Mono<SubscriptionContext> updateContextWithDate(Long userId, SubscriptionContext context, LocalDate selectedDate) {
        Subscription subscription = context.getSubscription();
        subscription.setPaymentDate(selectedDate);

        if (subscription.getId() == null) {
            subscription.setCurrency("RUB");
            return stateService.setContext(userId, context)
                    .thenReturn(stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_CONFIRMATION))
                    .thenReturn(context);
        }

        return stateService.setContext(userId, context)
                .then(stateService.clearState(userId))
                .thenReturn(context);
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
        InlineKeyboardMarkup keyboard = subscription.getId() == null
                ? keyboardService.getConfirmationKeyboard()
                : keyboardService.getEditConfirmationKeyboard(subscription.getId(), context.getPageNumber());

        return messageFactory.createEditMessage(
                query.getFrom().getId(),
                query.getMessage().getMessageId(),
                subscription,
                keyboard
        );
    }
}
