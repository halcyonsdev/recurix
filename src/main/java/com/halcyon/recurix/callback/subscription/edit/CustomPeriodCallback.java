package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.handler.subscription.edit.EditPeriodStepHandler;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import java.io.Serializable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает нажатие на кнопку "Другой период..." при редактировании подписки.
 * <p>
 * Этот обработчик является точкой входа для ввода пользователем кастомного
 * периода списания (в месяцах). Он переводит диалог в состояние
 * {@link ConversationState#AWAITING_NEW_PERIOD_MONTHS} и запрашивает у
 * пользователя текстовый ввод.
 *
 * @see EditPeriodStepHandler
 * @see BaseEditCallback
 */
@Component
public class CustomPeriodCallback extends BaseEditCallback {

    public CustomPeriodCallback(ConversationStateService stateService,
                                LocalMessageService messageService,
                                KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.PERIOD_SELECT_CUSTOM.equals(callbackData);
    }

    /**
     * Выполняет основную логику: устанавливает состояние и отправляет
     * пользователю запрос на ввод количества месяцев.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.period.custom_prompt",
                ConversationState.AWAITING_NEW_PERIOD_MONTHS,
                keyboardService.getBackToEditKeyboard());
    }
}
