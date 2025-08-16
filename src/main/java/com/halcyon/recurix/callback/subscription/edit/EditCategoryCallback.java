package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import java.io.Serializable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обработчик callback-запроса для начала редактирования категории подписки.
 * <p>
 * Срабатывает при нажатии на кнопку "Изменить категорию". Переводит пользователя
 * в состояние ожидания ввода текста новой категории.
 *
 * @see BaseEditCallback
 * @see com.halcyon.recurix.handler.subscription.edit.EditCategoryStepHandler
 */
@Component
public class EditCategoryCallback extends BaseEditCallback {

    protected EditCategoryCallback(ConversationStateService stateService,
                                   LocalMessageService messageService,
                                   KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_CATEGORY.equals(callbackData);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вызывает общую логику из {@link BaseEditCallback#sendEditMessage}, передавая
     * параметры, специфичные для изменения категории:
     * <ul>
     * <li>Ключ сообщения с запросом новой категории ({@code "dialog.add.edit.category"}).</li>
     * <li>Следующее состояние диалога ({@link ConversationState#AWAITING_NEW_CATEGORY}).</li>
     * </ul>
     *
     * @param update объект с данными от Telegram.
     * @return {@code Mono} с готовым
     *             {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText}
     *             для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.category",
                ConversationState.AWAITING_NEW_CATEGORY,
                keyboardService.getBackToEditKeyboard());
    }
}
