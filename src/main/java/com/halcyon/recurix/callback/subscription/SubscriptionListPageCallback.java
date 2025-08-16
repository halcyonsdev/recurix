package com.halcyon.recurix.callback.subscription;

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
import reactor.core.publisher.Mono;

/**
 * Обрабатывает callback-запросы для навигации между страницами списка подписок.
 * <p>
 * Этот класс отвечает за корректное отображение запрошенной страницы,
 * сохраняя при этом текущие настройки сортировки, выбранные пользователем.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionListPageCallback implements Callback {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionMessageFactory subscriptionMessageFactory;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_LIST_PAGE_PREFIX);
    }

    /**
     * Загружает и отображает указанную страницу подписок пользователя.
     * <p>
     * Метод получает текущие настройки сортировки пользователя из Redis,
     * запрашивает соответствующую страницу данных и обновляет исходное
     * сообщение новым контентом и клавиатурой пагинации.
     *
     * @param update Входящий объект Update, содержащий callback-запрос.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        int pageNumber = Integer.parseInt(callbackQuery.getData().substring(CallbackData.SUB_LIST_PAGE_PREFIX.length()));

        log.info("User {} requested subscription list page #{}", callbackQuery.getFrom().getId(), pageNumber);

        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.DEFAULT_PAGE_SIZE, PaginationConstants.DEFAULT_SORT);
        var defaultContext = new SubscriptionListContext("paymentDate", Sort.Direction.ASC);

        return userService.findOrCreateUser(callbackQuery.getFrom())
                .flatMap(user -> subscriptionService.getSubscriptionsAsPage(user.id(), pageable))
                .map(page -> EditMessageText.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .messageId(messageId)
                        .text(subscriptionMessageFactory.formatSubscriptionsPage(page, messageId))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(keyboardService.getSubscriptionsPageKeyboard(page, defaultContext))
                        .build());
    }
}
