package com.halcyon.recurix.callback.subscription.add;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.message.SubscriptionMessageFactory;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.pagination.PaginationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для отмены процесса добавления подписки.
 * <p>
 * Срабатывает при нажатии на кнопку "Отмена". Завершает диалог, очищает
 * состояние пользователя и возвращает его к списку всех подписок.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CancelSubscriptionCallback implements Callback {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final TelegramApiClient telegramApiClient;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.SUBSCRIPTION_CANCEL.equals(callbackData);
    }

    /**
     * Выполняет отмену диалога добавления подписки.
     * <p>
     * Метод выполняет следующие действия в реактивной последовательности:
     * <ol>
     * <li>Отправляет пользователю всплывающее уведомление о том, что операция отменена.</li>
     * <li>Полностью очищает состояние и контекст диалога пользователя в Redis.</li>
     * <li>Загружает актуальный список всех подписок пользователя.</li>
     * <li>Удаляет прошлое сообщение и отправляет первую страницу списка подписок.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом
     *             {@link org.telegram.telegrambots.meta.api.methods.send.SendMessage} со списком
     *             подписок.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} cancelled the subscription creation process.", telegramUser.getId());

        Mono<RecurixUser> userMono = stateService.endConversation(chatId)
                .then(userService.findOrCreateUser(telegramUser));

        return telegramApiClient.sendAnswerCallbackQuery(
                callbackQuery.getId(),
                messageService.getMessage("dialog.confirm.cancelled"))
                .then(telegramApiClient.deleteMessage(chatId, messageId))
                .then(userMono.flatMap(user -> {
                    Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE,
                            PaginationConstants.DEFAULT_SORT);
                    return subscriptionService.getSubscriptionsAsPage(user.id(), pageable);
                }))
                .map(page -> subscriptionMessageFactory.createNewSubscriptionsPageMessage(chatId, messageId, page));
    }
}
