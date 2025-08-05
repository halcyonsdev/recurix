package com.halcyon.recurix.callback.subscription.add.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.support.SubscriptionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик для кнопки "Назад", позволяющей вернуться к главному меню редактирования подписки.
 * <p>
 * Срабатывает, когда пользователь находится в режиме ввода конкретного поля (например, названия)
 * и решает вернуться к списку всех полей для редактирования.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackToEditCallback implements Callback {

    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.BACK_TO_EDIT.equals(callbackData);
    }

    /**
     * Возвращает пользователя к основному экрану редактирования подписки.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     *     <li>Извлекает данные из callback-запроса.</li>
     *     <li>Загружает из Redis актуальный контекст с данными создаваемой подписки.</li>
     *     <li>Формирует и отправляет сообщение со списком полей для редактирования и соответствующей клавиатурой.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} is returning to the main edit menu.", userId);

        return stateService.getContext(userId, SubscriptionContext.class)
                .map(context -> subscriptionMessageFactory.createEditMessage(
                        userId,
                        messageId,
                        context.getSubscription(),
                        keyboardService.getEditKeyboard()
                ));
    }
}
