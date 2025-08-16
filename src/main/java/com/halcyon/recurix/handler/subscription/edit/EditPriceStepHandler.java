package com.halcyon.recurix.handler.subscription.edit;

import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.support.InputParser;
import com.halcyon.recurix.support.PeriodFormatter;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EditPriceStepHandler extends BaseEditStepHandler<BigDecimal> {

    private final InputParser inputParser;

    public EditPriceStepHandler(
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
        return ConversationState.AWAITING_NEW_PRICE.equals(state);
    }

    @Override
    protected BigDecimal parse(String text) {
        return inputParser.parsePrice(text);
    }

    @Override
    protected void updateSubscription(Subscription subscription, BigDecimal value) {
        subscription.setPrice(value);
    }
}
