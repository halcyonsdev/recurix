package com.halcyon.recurix.command;

import com.halcyon.recurix.support.SubscriptionMessageFactory;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

import java.io.Serializable;


/**
 * Обработчик команды /list, отвечающий за отображение списка всех подписок пользователя.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListCommand implements BotCommand {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    private static final String LIST_COMMAND = "/list";

    @Override
    public boolean supports(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().getText().equals(LIST_COMMAND);
    }

    /**
     * Выполняет логику команды /list.
     * <p>
     * Метод находит или создает пользователя в базе данных, запрашивает для него
     * все сохраненные подписки, форматирует их в виде читаемого списка и отправляет
     * пользователю в новом сообщении вместе с соответствующей клавиатурой.
     *
     * @param update Объект с сообщением от пользователя.
     * @return {@code Mono} с объектом {@link SendMessage}, содержащим список подписок.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        User telegramUser = update.getMessage().getFrom();
        Long chatId = update.getMessage().getChatId();

        log.info("User {} executed /list command.", telegramUser.getId());

        return userService.findOrCreateUser(telegramUser)
                .flatMap(user -> subscriptionService.getAllByUserId(user.id()).collectList())
                .map(subscriptions -> SendMessage.builder()
                            .chatId(chatId)
                            .text(subscriptionMessageFactory.formatSubscriptionList(subscriptions))
                            .parseMode(ParseMode.MARKDOWN)
                            .replyMarkup(keyboardService.getSubscriptionsKeyboard())
                            .build());
    }

}
