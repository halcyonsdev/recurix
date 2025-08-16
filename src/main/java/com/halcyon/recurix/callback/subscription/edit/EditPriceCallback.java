package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.handler.subscription.edit.EditPriceStepHandler;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для начала редактирования цены подписки.
 * <p>
 * Срабатывает при нажатии на кнопку "Изменить цену". Переводит пользователя
 * в состояние ожидания ввода текста новой цены.
 *
 * @see BaseEditCallback
 * @see EditPriceStepHandler
 */
@Component
public class EditPriceCallback extends BaseEditCallback {

    public EditPriceCallback(ConversationStateService stateService, LocalMessageService messageService, KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_PRICE.equals(callbackData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вызывает общую логику из {@link BaseEditCallback#sendEditMessage}, передавая
     * параметры, специфичные для изменения цены:
     * <ul>
     *     <li>Ключ сообщения с запросом новой цены ({@code "dialog.add.edit.price"}).</li>
     *     <li>Следующее состояние диалога ({@link ConversationState#AWAITING_NEW_PRICE}).</li>
     * </ul>
     *
     * @param update объект с данными от Telegram.
     * @return {@code Mono} с готовым {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText} для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.price",
                ConversationState.AWAITING_NEW_PRICE,
                keyboardService.getBackToEditKeyboard()
        );
    }
}
