package com.halcyon.recurix.handler.subscription.edit;

import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.handler.ConversationStepHandler;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import com.halcyon.recurix.support.PeriodFormatter;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

/**
 * Абстрактный базовый класс для обработчиков шагов редактирования подписки.
 * Инкапсулирует общую логику: получение контекста, выполнение побочных эффектов
 * (удаление сообщений, смена состояния) и формирование ответного сообщения.
 *
 * @param <T> Тип данных, получаемых от пользователя (например, String для имени, BigDecimal для
 *            цены).
 */
public abstract class BaseEditStepHandler<T> implements ConversationStepHandler {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final TelegramApiClient telegramApiClient;
    private final PeriodFormatter periodFormatter;

    protected BaseEditStepHandler(
                                  ConversationStateService stateService,
                                  LocalMessageService messageService,
                                  KeyboardService keyboardService,
                                  TelegramApiClient telegramApiClient,
                                  PeriodFormatter periodFormatter) {
        this.stateService = stateService;
        this.messageService = messageService;
        this.keyboardService = keyboardService;
        this.telegramApiClient = telegramApiClient;
        this.periodFormatter = periodFormatter;
    }

    /**
     * Парсит и валидирует текстовый ввод от пользователя.
     * 
     * @param text Входной текст.
     * @return Распарсенное и провалидированное значение типа T.
     * @throws InvalidInputException если ввод некорректен.
     */
    protected abstract T parse(String text) throws InvalidInputException;

    /**
     * Обновляет соответствующее поле в объекте Subscription.
     * 
     * @param subscription Объект подписки для обновления.
     * @param value        Значение, которое нужно установить.
     */
    protected abstract void updateSubscription(Subscription subscription, T value);

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        Integer messageId = update.getMessage().getMessageId();
        String text = update.getMessage().getText();

        try {
            T parsedValue = parse(text);

            return stateService.getContext(userId, SubscriptionContext.class)
                    .flatMap(context -> {
                        Mono<Void> sideEffects = performSideEffects(context, userId, messageId,
                                subscription -> updateSubscription(subscription, parsedValue));

                        return sideEffects.thenReturn(context);
                    })
                    .map(context -> createEditMessage(userId, context));
        } catch (InvalidInputException e) {
            return Mono.just(new SendMessage(
                    userId.toString(),
                    messageService.getMessage(e.getMessageCode())));
        } catch (Exception e) {
            return Mono.just(new SendMessage(
                    userId.toString(),
                    messageService.getMessage("error.unexpected")));
        }
    }

    private Mono<Void> performSideEffects(
                                          SubscriptionContext context,
                                          Long userId,
                                          Integer messageId,
                                          Consumer<Subscription> subscriptionUpdater) {
        Mono<Void> deleteUserMessageMono = telegramApiClient.deleteMessage(userId, messageId);

        subscriptionUpdater.accept(context.getSubscription());

        Mono<Void> saveContextMono = stateService.setContext(userId, context);
        Mono<Void> clearStateMono = stateService.clearState(userId);

        return Mono.when(deleteUserMessageMono, saveContextMono, clearStateMono);
    }

    private EditMessageText createEditMessage(Long userId, SubscriptionContext context) {
        Subscription subscription = context.getSubscription();
        String summary = messageService.getMessage(
                "dialog.add.prompt.confirmation",
                subscription.getName(),
                subscription.getPrice(),
                subscription.getPaymentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                subscription.getCategory(),
                periodFormatter.format(subscription.getRenewalMonths()));

        InlineKeyboardMarkup keyboard = subscription.getId() == null
                ? keyboardService.getConfirmationKeyboard()
                : keyboardService.getEditConfirmationKeyboard(subscription.getId(), context.getPageNumber());

        return EditMessageText.builder()
                .chatId(userId.toString())
                .messageId(context.getMessageToEditId())
                .text(summary)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboard)
                .build();
    }
}
