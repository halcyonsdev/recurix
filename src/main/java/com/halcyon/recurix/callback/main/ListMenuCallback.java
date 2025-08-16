package com.halcyon.recurix.callback.main;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.PaginationConstants;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

/**
 * Обработчик callback-запроса для отображения списка подписок пользователя.
 * <p>
 * Срабатывает при нажатии на кнопку "Мои подписки" в главном меню.
 * Загружает подписки из базы данных и форматирует их в виде сообщения.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListMenuCallback implements Callback {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.MENU_SUBSCRIPTIONS.equals(callbackData);
    }

    /**
     * Загружает и отображает список всех подписок пользователя.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Извлекает информацию о пользователе, чате и сообщении из объекта {@code Update}.</li>
     * <li>Находит или создает пользователя в базе данных.</li>
     * <li>Получает все подписки этого пользователя для первой страницы.</li>
     * <li>Использует {@link SubscriptionMessageFactory} для создания отформатированного сообщения со
     * списком.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText},
     *             который обновляет исходное сообщение, отображая список подписок.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Long userId = telegramUser.getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} requested their subscription list.", userId);

        Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE, PaginationConstants.DEFAULT_SORT);
        var defaultContext = new SubscriptionListContext("paymentDate", Sort.Direction.ASC);

        return userService.findOrCreateUser(telegramUser)
                .flatMap(user -> subscriptionService.getSubscriptionsAsPage(user.id(), pageable))
                .map(page -> EditMessageText.builder()
                        .chatId(userId)
                        .messageId(messageId)
                        .text(subscriptionMessageFactory.formatSubscriptionsPage(page, messageId))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(keyboardService.getSubscriptionsPageKeyboard(page, defaultContext))
                        .build());
    }
}
