package com.halcyon.recurix.handler.subscription.edit;

import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.support.PeriodFormatter;
import org.springframework.stereotype.Component;

@Component
public class EditNameStepHandler extends BaseEditStepHandler<String> {

    public EditNameStepHandler(ConversationStateService stateService,
                               LocalMessageService messageService,
                               KeyboardService keyboardService,
                               TelegramApiClient telegramApiClient,
                               PeriodFormatter periodFormatter) {
        super(stateService, messageService, keyboardService, telegramApiClient, periodFormatter);
    }

    @Override
    public boolean supports(ConversationState state) {
        return ConversationState.AWAITING_NEW_NAME.equals(state);
    }

    @Override
    protected String parse(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("error.format.name", "Name cannot be empty");
        }

        return text;
    }

    @Override
    protected void updateSubscription(Subscription subscription, String value) {
        subscription.setName(value);
    }
}
