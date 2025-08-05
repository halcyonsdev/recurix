package com.halcyon.recurix.handler.subscription;

import com.halcyon.recurix.exception.InvalidInputException;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Обрабатывает ввод цены подписки на втором шаге диалога при создании подписки.
 */
@Component
@RequiredArgsConstructor
public class PriceStepHandler implements ConversationStepHandler {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final InputParser inputParser;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(ConversationState state) {
        return ConversationState.AWAITING_SUBSCRIPTION_PRICE.equals(state);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Выполняет следующую логику:
     * 1. Асинхронно удаляет сообщение пользователя с введенной ценой.
     * 2. Парсит и валидирует введенный текст.
     * 3. В случае успеха, обновляет цену в контексте и переводит диалог на следующий шаг.
     * 4. Редактирует исходное сообщение диалога, задавая следующий вопрос ("Введите дату").
     * 5. В случае ошибки парсинга, отправляет уведомление об ошибке.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        String inputText = update.getMessage().getText();

        Mono<SendMessage> logicMono = Mono.fromCallable(() -> inputParser.parsePrice(inputText))
                .flatMap(price ->
                        updateContextAndState(userId, price)
                                .then(createNextStepMessage(userId))
                )
                .onErrorResume(InvalidInputException.class, e ->
                        subscriptionMessageFactory.createErrorMessage(userId, e)
                );

        return logicMono.map(sendMessage -> sendMessage);
    }

    /**
     * Обновляет контекст новой ценой и переводит диалог в следующее состояние.
     * @param userId ID пользователя.
     * @param price Новая цена.
     * @return Mono<Void>, который завершается после сохранения контекста и состояния.
     */
    private Mono<Void> updateContextAndState(Long userId, BigDecimal price) {
        return stateService.getContext(userId, SubscriptionContext.class)
                .flatMap(context -> {
                    context.getSubscription().setPrice(price);

                    return Mono.when(
                            stateService.setContext(userId, context),
                            stateService.setState(userId, ConversationState.AWAITING_SUBSCRIPTION_DATE)
                    );
                });
    }

    /**
     * Создает новое сообщение для следующего шага диалога (запрос даты).
     *
     * @param userId ID чата.
     * @return Готовый объект SendMessage.
     */
    private Mono<SendMessage> createNextStepMessage(Long userId) {
        return Mono.fromCallable(() -> SendMessage.builder()
                .chatId(userId.toString())
                .text(messageService.getMessage("dialog.add.prompt.date"))
                .replyMarkup(keyboardService.getBackToMenuKeyboard())
                .build());
    }
}
