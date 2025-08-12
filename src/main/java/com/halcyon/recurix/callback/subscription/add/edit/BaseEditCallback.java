package com.halcyon.recurix.callback.subscription.add.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.handler.ConversationState;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Абстрактный базовый класс для обработчиков callback-запросов в меню редактирования подписки.
 * <p>
 * Предоставляет общую логику для отправки пользователю сообщений с запросом на ввод
 * данных (например, нового имени или цены) и для отображения сообщений с кастомными клавиатурами.
 */
@Slf4j
public abstract class BaseEditCallback implements Callback {

    protected final ConversationStateService stateService;
    protected final LocalMessageService messageService;
    protected final KeyboardService keyboardService;

    protected BaseEditCallback(ConversationStateService stateService, LocalMessageService messageService, KeyboardService keyboardService) {
        this.stateService = stateService;
        this.messageService = messageService;
        this.keyboardService = keyboardService;
    }

    /**
     * Переводит диалог в новое состояние и отправляет пользователю сообщение с запросом на ввод данных.
     * <p>
     * Этот универсальный метод используется для редактирования полей, требующих текстового ввода от пользователя
     * (например, названия, цены, даты).
     *
     * @param update      Входящий объект {@link Update} с {@link CallbackQuery}.
     * @param messageCode Ключ сообщения из файла properties для текста запроса (например, "dialog.add.edit.name").
     * @param nextState   Состояние {@link ConversationState}, в которое нужно перевести диалог для ожидания ввода.
     * @return {@code Mono}, содержащий готовый объект {@link EditMessageText} для отправки пользователю.
     */
    protected Mono<BotApiMethod<? extends Serializable>> sendEditMessage(Update update, String messageCode, ConversationState nextState) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Long userId = telegramUser.getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} is entering state {} to edit a subscription field.", userId, nextState);

        return stateService.setState(userId, nextState)
                .then(
                        stateService.getContext(userId, SubscriptionContext.class)
                                .flatMap(context -> {
                                    context.setMessageToEditId(messageId);
                                    return stateService.setContext(userId, context);
                                })
                )
                .then(Mono.fromCallable(() ->
                        EditMessageText.builder()
                                .chatId(userId)
                                .messageId(messageId)
                                .text(messageService.getMessage(messageCode))
                                .replyMarkup(keyboardService.getBackToEditKeyboard())
                                .build()
                ));
    }

    /**
     * Отправляет пользователю сообщение с кастомной клавиатурой без изменения состояния диалога.
     * <p>
     * Используется для случаев, когда выбор осуществляется нажатием на кнопку, а не вводом текста
     * (например, выбор валюты).
     *
     * @param update       Входящий объект {@link Update}.
     * @param messageCode  Ключ сообщения для текста запроса.
     * @param keyboard     Объект {@link InlineKeyboardMarkup} с кнопками для выбора.
     * @return {@code Mono}, содержащий готовый объект {@link EditMessageText}.
     */
    protected Mono<BotApiMethod<? extends Serializable>> sendEditRequestWithCustomKeyboard(
            Update update,
            String messageCode,
            InlineKeyboardMarkup keyboard
    ) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Long userId = telegramUser.getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        return Mono.fromCallable(() ->
                EditMessageText.builder()
                        .chatId(userId)
                        .messageId(messageId)
                        .text(messageService.getMessage(messageCode))
                        .replyMarkup(keyboard)
                        .build()
        );
    }
}
