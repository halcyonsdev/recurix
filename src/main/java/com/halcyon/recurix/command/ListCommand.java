package com.halcyon.recurix.command;

import com.halcyon.recurix.message.SubscriptionMessageFactory;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.pagination.PaginationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
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
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    private static final String LIST_COMMAND = "/list";

    @Override
    public boolean supports(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().getText().equals(LIST_COMMAND);
    }

    /**
     * Обработчик команды /list, отвечающий за отображение первой страницы списка подписок пользователя.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Находит или создает пользователя в базе данных.</li>
     * <li>Формирует запрос ({@link org.springframework.data.domain.Pageable}) для получения первой
     * страницы (страница 0)
     * с использованием стандартных настроек сортировки и размера страницы из
     * {@link com.halcyon.recurix.service.pagination.PaginationConstants}.</li>
     * <li>Запрашивает у сервиса пагинированный список подписок.</li>
     * <li>С помощью {@link SubscriptionMessageFactory} создает новое
     * сообщение,
     * содержащее отформатированную страницу и клавиатуру для навигации.</li>
     * </ol>
     *
     * @param update Объект с сообщением от пользователя.
     * @return {@code Mono} с объектом {@link SendMessage}, содержащим первую страницу подписок и
     *             клавиатуру для пагинации.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        User telegramUser = update.getMessage().getFrom();

        log.info("User {} executed /list command.", telegramUser.getId());

        return userService.findOrCreateUser(telegramUser)
                .flatMap(user -> {
                    Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE,
                            PaginationConstants.DEFAULT_SORT);
                    return subscriptionService.getSubscriptionsAsPage(user.id(), pageable);
                })
                .map(page -> subscriptionMessageFactory.createNewSubscriptionsPageMessage(
                        update.getMessage().getChatId(),
                        update.getMessage().getMessageId(),
                        page));
    }
}
