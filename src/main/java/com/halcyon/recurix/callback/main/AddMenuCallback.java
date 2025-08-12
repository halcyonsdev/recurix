package com.halcyon.recurix.callback.main;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для начала диалога по добавлению новой подписки.
 * Срабатывает при нажатии на кнопку "Добавить подписку" в главном меню.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AddMenuCallback implements Callback {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.MENU_ADD_SUBSCRIPTION.equals(callbackData);
    }

    /**
     * Инициирует процесс добавления новой подписки.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     *     <li>Логирует начало процесса.</li>
     *     <li>Создает и сохраняет в Redis пустой контекст {@link SubscriptionContext} для диалога.</li>
     *     <li>Переводит пользователя в состояние {@link ConversationState#AWAITING_SUBSCRIPTION_NAME}.</li>
     *     <li>Отправляет пользователю сообщение с запросом на ввод названия подписки.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления исходного сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getCallbackQuery().getFrom().getId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        log.info("User {} starts adding a new subscription", userId);

        var context = new SubscriptionContext(new Subscription(), messageId);

        Mono<Void> initializeConversation = stateService.setContext(userId, context)
                .then(stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_NAME));

        return initializeConversation.then(Mono.fromCallable(() ->
                EditMessageText.builder()
                        .chatId(userId)
                        .messageId(messageId)
                        .text(messageService.getMessage("dialog.add.prompt.name"))
                        .replyMarkup(keyboardService.getBackToMenuKeyboard())
                        .build()
        ));
    }
}
