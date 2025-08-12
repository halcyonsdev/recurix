package com.halcyon.recurix.handler.subscription;

import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.handler.ConversationStepHandler;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class NameStepHandler implements ConversationStepHandler {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(ConversationState state) {
        return ConversationState.AWAITING_SUBSCRIPTION_NAME.equals(state);
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText();

        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    context.getSubscription().setName(text);

                    return stateService.setContext(userId, context)
                            .then(stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_PRICE));
                }).then(
                        Mono.fromCallable(() -> SendMessage.builder()
                                .chatId(userId)
                                .text(messageService.getMessage("dialog.add.prompt.price"))
                                .replyMarkup(keyboardService.getBackToMenuKeyboard())
                                .build()
                ));
    }
}
