package com.halcyon.recurix.handler.subscription;

import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.handler.ConversationStepHandler;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.support.InputParser;
import com.halcyon.recurix.support.SubscriptionContext;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class DateStepHandler implements ConversationStepHandler {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final InputParser inputParser;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(ConversationState state) {
        return ConversationState.AWAITING_SUBSCRIPTION_DATE.equals(state);
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();

        return Mono.fromCallable(() -> inputParser.parseDate(text))
                .flatMap(date ->
                        updateContextAndClearState(userId, date)
                                .map(subscription -> createConfirmationMessage(userId, subscription)))
                .onErrorResume(InvalidInputException.class, e ->
                        subscriptionMessageFactory.createErrorMessage(userId, e)
                )
                .map(sendMessage -> sendMessage);
    }

    private Mono<Subscription> updateContextAndClearState(Long userId, LocalDate date) {
        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    context.getSubscription().setPaymentDate(date);
                    context.getSubscription().setCurrency("RUB");

                    return Mono.when(
                            stateService.setContext(userId, context),
                            stateService.clearState(userId)
                    ).thenReturn(context.getSubscription());
                });
    }

    private SendMessage createConfirmationMessage(Long userId, Subscription subscription) {
        String summary = messageService.getMessage(
                "dialog.add.prompt.confirmation",
                subscription.getName(),
                subscription.getPrice(),
                subscription.getPaymentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                subscription.getCurrency(),
                subscription.getCategory()
        );

        var response = new SendMessage(userId.toString(), summary);
        response.setReplyMarkup(keyboardService.getConfirmationKeyboard());
        response.setParseMode(ParseMode.MARKDOWN);

        return response;
    }
}
