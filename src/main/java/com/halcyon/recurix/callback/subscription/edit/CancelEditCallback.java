package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.callback.subscription.SubscriptionViewCallback;
import com.halcyon.recurix.callback.subscription.UpdateSubscriptionCallback;
import com.halcyon.recurix.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обрабатывает нажатие кнопки "Отменить" на экране подтверждения редактирования.
 * <p>
 * Этот обработчик прерывает сеанс редактирования, не сохраняя никаких изменений.
 * Он очищает состояние диалога пользователя в Redis и возвращает его
 * к экрану детального просмотра подписки.
 *
 * @see UpdateSubscriptionCallback
 * @see SubscriptionViewCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CancelEditCallback implements Callback {

    private final ConversationStateService stateService;
    private final SubscriptionViewCallback viewCallback;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_CANCEL_EDIT_AND_VIEW_PREFIX);
    }

    /**
     * Выполняет основную логику: завершает диалог и возвращает к детальному просмотру.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText}
     *         для перерисовки экрана детального просмотра.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        log.info("User {} cancelled subscription editing.", userId);

        return stateService.endConversation(userId)
                .then(viewCallback.execute(update, CallbackData.SUB_CANCEL_EDIT_AND_VIEW_PREFIX));
    }
}
