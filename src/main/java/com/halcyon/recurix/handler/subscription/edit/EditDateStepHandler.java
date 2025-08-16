package com.halcyon.recurix.handler.subscription.edit;

import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.support.InputParser;
import com.halcyon.recurix.support.PeriodFormatter;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class EditDateStepHandler extends BaseEditStepHandler<LocalDate> {

    private final InputParser inputParser;

    public EditDateStepHandler(
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
        return ConversationState.AWAITING_NEW_DATE.equals(state);
    }

    @Override
    protected LocalDate parse(String text) {
        return inputParser.parseDate(text);
    }

    @Override
    protected void updateSubscription(Subscription subscription, LocalDate value) {
        subscription.setPaymentDate(value);
    }
}
