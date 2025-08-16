package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает нажатие на кнопку "Изменить период" в меню редактирования.
 * <p>
 * Его задача — предоставить пользователю выбор между стандартными периодами
 * (ежемесячно/ежегодно) и вводом собственного значения.
 *
 * @see ChoosePeriodCallback
 * @see CustomPeriodCallback
 */
@Component
@Slf4j
public class EditPeriodCallback extends BaseEditCallback {

    public EditPeriodCallback(ConversationStateService stateService,
                              LocalMessageService messageService,
                              KeyboardService keyboardService) {
        super(stateService, messageService, keyboardService);
    }

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.EDIT_PERIOD.equals(callbackData);
    }

    /**
     * Выполняет основную логику: отправляет пользователю сообщение с
     * клавиатурой выбора периода списания.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с
     *             {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText}
     *             для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return sendEditMessage(
                update,
                "dialog.add.edit.period.prompt",
                null,
                keyboardService.getPeriodSelectionKeyboard());
    }
}
