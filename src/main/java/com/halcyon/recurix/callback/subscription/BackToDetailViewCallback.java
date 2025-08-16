package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.callback.subscription.edit.EditFromDetailViewCallback;
import com.halcyon.recurix.service.ConversationStateService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает нажатие кнопки "Назад" из меню редактирования подписки.
 * <p>
 * Основная задача этого обработчика — безопасно прервать сеанс редактирования,
 * очистив состояние диалога в Redis, и вернуть пользователя на экран
 * детального просмотра подписки, который был до начала редактирования.
 *
 * @see EditFromDetailViewCallback
 * @see SubscriptionViewCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackToDetailViewCallback implements Callback {

    private final ConversationStateService stateService;
    private final SubscriptionViewCallback viewCallback;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_BACK_TO_VIEW_PREFIX);
    }

    /**
     * Выполняет основную логику: завершает диалог и перерисовывает детальный вид.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText}
     *             для возврата к детальному просмотру.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        log.info("User {} is returning to the detail view from the edit menu.", userId);

        return stateService.endConversation(userId)
                .then(viewCallback.execute(update, CallbackData.SUB_BACK_TO_VIEW_PREFIX));
    }
}
