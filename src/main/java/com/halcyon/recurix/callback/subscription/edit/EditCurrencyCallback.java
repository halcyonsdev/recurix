package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обрабатывает нажатие на кнопку "Изменить валюту" в меню редактирования.
 * <p>
 * Этот класс не устанавливает состояние ожидания текстового ввода.
 * Вместо этого он отправляет пользователю сообщение с инлайн-клавиатурой для выбора одной из доступных валют.
 *
 * @see BaseEditCallback
 * @see ChooseCurrencyCallback
 */
@Component
public class EditCurrencyCallback extends BaseEditCallback {

    public EditCurrencyCallback(ConversationStateService stateService, LocalMessageService messageService, KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_CURRENCY.equals(callbackData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вызывает общую логику из {@link BaseEditCallback#sendEditMessage},
     * чтобы отправить пользователю сообщение с инлайн-клавиатурой для выбора валюты.
     *
     * @param update объект с данными от Telegram.
     * @return {@code Mono} с готовым {@link EditMessageText} для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.currency",
                ConversationState.AWAITING_NEW_CURRENCY,
                keyboardService.getCurrencySelectionKeyboard()
        );
    }
}
