package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.handler.subscription.edit.EditNameStepHandler;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для начала редактирования названия подписки.
 * <p>
 * Срабатывает при нажатии на кнопку "Изменить название". Переводит пользователя
 * в состояние ожидания ввода текста нового названия.
 *
 * @see BaseEditCallback
 * @see EditNameStepHandler
 */
@Component
public class EditNameCallback extends BaseEditCallback {

    public EditNameCallback(ConversationStateService stateService, LocalMessageService messageService, KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_NAME.equals(callbackData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вызывает общую логику из {@link BaseEditCallback#sendEditMessage}, передавая
     * параметры, специфичные для изменения имени:
     * <ul>
     *     <li>Ключ сообщения с запросом нового названия ({@code "dialog.add.edit.name"}).</li>
     *     <li>Следующее состояние диалога ({@link ConversationState#AWAITING_NEW_NAME}).</li>
     * </ul>
     *
     * @param update объект с данными от Telegram.
     * @return {@code Mono} с готовым {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText} для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.name",
                ConversationState.AWAITING_NEW_NAME,
                keyboardService.getBackToEditKeyboard()
        );
    }
}
