package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.callback.subscription.edit.CancelEditCallback;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает нажатие кнопки "Сохранить изменения" после редактирования подписки.
 * <p>
 * Этот класс является финальным шагом в успешном сценарии редактирования.
 * Он выполняет следующие действия:
 * <ol>
 * <li>Извлекает измененные данные подписки из {@link SubscriptionContext} в Redis.</li>
 * <li>Сохраняет (обновляет) эти данные в базе данных.</li>
 * <li>Полностью очищает состояние диалога редактирования из Redis.</li>
 * <li>Возвращает пользователя на экран детального просмотра с уже обновленными данными.</li>
 * </ol>
 *
 * @see CancelEditCallback
 * @see SubscriptionViewCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateSubscriptionCallback implements Callback {

    private final ConversationStateService stateService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionViewCallback viewCallback;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_UPDATE_AND_VIEW_PREFIX);
    }

    /**
     * Запускает процесс сохранения изменений и обновления вида.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText}
     *             для отображения обновленного детального вида.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    Subscription subscriptionToSave = context.getSubscription();
                    log.info("User {} is saving changes to subscription {}", userId, subscriptionToSave.getId());

                    return subscriptionService.save(subscriptionToSave);
                })
                .flatMap(savedSubscription -> stateService.endConversation(userId)
                        .then(viewCallback.execute(update, CallbackData.SUB_UPDATE_AND_VIEW_PREFIX)));
    }
}
