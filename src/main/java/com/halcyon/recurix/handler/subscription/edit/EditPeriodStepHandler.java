package com.halcyon.recurix.handler.subscription.edit;

import com.halcyon.recurix.callback.subscription.edit.CustomPeriodCallback;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.support.InputParser;
import com.halcyon.recurix.support.PeriodFormatter;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает текстовый ввод пользователя для установки кастомного периода подписки.
 * <p>
 * Срабатывает после того, как пользователь выбрал "Другой период..." и ввел
 * количество месяцев. Этот обработчик парсит введенное число, обновляет
 * подписку в контексте диалога и возвращает пользователя на экран подтверждения.
 *
 * @see CustomPeriodCallback
 * @see BaseEditStepHandler
 */
@Component
public class EditPeriodStepHandler extends BaseEditStepHandler<Integer> {

    private final InputParser inputParser;

    public EditPeriodStepHandler(
                                 ConversationStateService stateService,
                                 LocalMessageService messageService,
                                 KeyboardService keyboardService,
                                 TelegramApiClient telegramApiClient,
                                 PeriodFormatter periodFormatter,
                                 InputParser inputParser) {
        super(stateService, messageService, keyboardService, telegramApiClient, periodFormatter);
        this.inputParser = inputParser;
    }

    @Override
    public boolean supports(ConversationState state) {
        return ConversationState.AWAITING_NEW_PERIOD_MONTHS.equals(state);
    }

    @Override
    protected Integer parse(String text) throws InvalidInputException {
        return inputParser.parsePeriodMonths(text);
    }

    @Override
    protected void updateSubscription(Subscription subscription, Integer value) {
        subscription.setRenewalMonths(value);
    }
}
