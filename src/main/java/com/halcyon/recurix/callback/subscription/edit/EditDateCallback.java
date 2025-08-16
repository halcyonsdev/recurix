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
import java.time.YearMonth;

/**
 * Обработчик callback-запроса для начала редактирования даты следующего платежа.
 * <p>
 * Срабатывает при нажатии на кнопку "Изменить дату". Переводит пользователя
 * в состояние ожидания ввода текста новой даты.
 *
 * @see BaseEditCallback
 * @see com.halcyon.recurix.handler.subscription.edit.EditDateStepHandler
 */
@Component
public class EditDateCallback extends BaseEditCallback {

    public EditDateCallback(ConversationStateService stateService, LocalMessageService messageService, KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_DATE.equals(callbackData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вызывает общую логику из {@link BaseEditCallback#sendEditMessage}, передавая
     * параметры, специфичные для изменения даты:
     * <ul>
     *     <li>Ключ сообщения с запросом новой даты ({@code "dialog.add.edit.date"}).</li>
     *     <li>Следующее состояние диалога ({@link ConversationState#AWAITING_NEW_DATE}).</li>
     * </ul>
     *
     * @param update объект с данными от Telegram.
     * @return {@code Mono} с готовым {@link EditMessageText} для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.prompt.date",
                ConversationState.AWAITING_NEW_DATE,
                keyboardService.getCalendarKeyboard(YearMonth.now(), CallbackData.BACK_TO_EDIT)
        );
    }
}
