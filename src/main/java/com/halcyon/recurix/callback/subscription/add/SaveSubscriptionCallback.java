package com.halcyon.recurix.callback.subscription.add;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.message.SubscriptionMessageFactory;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.context.SubscriptionContext;
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
import reactor.util.function.Tuple2;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для сохранения новой подписки.
 * <p>
 * Срабатывает при нажатии на кнопку "Сохранить" на экране подтверждения.
 * Завершает диалог, сохраняет данные в базу, очищает состояние и
 * отображает обновленный список всех подписок.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaveSubscriptionCallback implements Callback {

    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final TelegramApiClient telegramApiClient;
    private final SubscriptionMessageFactory subscriptionMessageFactory;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.SUBSCRIPTION_SAVE.equals(callbackData);
    }

    /**
     * Запускает процесс сохранения подписки и отображения результата.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     * <li>Отправляет пользователю всплывающее уведомление об успешном сохранении.</li>
     * <li>Вызывает приватный метод для извлечения данных из контекста и сохранения их в БД.</li>
     * <li>Полностью завершает диалог, очищая состояние и контекст в Redis.</li>
     * <li>Загружает и отображает обновленный список всех подписок пользователя.</li>
     * </ol>
     *
     * @param update Входящее обновление от Telegram с {@link CallbackQuery}.
     * @return {@code Mono}, содержащий
     *             {@link org.telegram.telegrambots.meta.api.methods.send.SendMessage} с первой
     *             страницей списка подписок.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} initiated saving a subscription.", telegramUser.getId());

        Mono<Long> userMono = saveSubscriptionFromContext(telegramUser)
                .flatMap(savedSubscription -> stateService.endConversation(telegramUser.getId())
                        .thenReturn(savedSubscription.getUserId()));

        return telegramApiClient.sendAnswerCallbackQuery(callbackQuery.getId(), messageService.getMessage("add.success"))
                .then(telegramApiClient.deleteMessage(chatId, messageId))
                .then(userMono.flatMap(userId -> {
                    Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE,
                            PaginationConstants.DEFAULT_SORT);
                    return subscriptionService.getSubscriptionsAsPage(userId, pageable);
                }))
                .map(page -> subscriptionMessageFactory.createNewSubscriptionsPageMessage(chatId, messageId, page));
    }

    /**
     * Извлекает данные из контекста диалога, связывает их с пользователем и сохраняет новую подписку в
     * базе данных.
     *
     * @param telegramUser пользователь Telegram, для которого сохраняется подписка.
     * @return {@code Mono}, содержащий сохраненный объект {@link Subscription}, или
     *             {@code Mono.empty()}, если контекст не найден.
     */
    private Mono<Subscription> saveSubscriptionFromContext(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        Mono<RecurixUser> userMono = userService.findOrCreateUser(telegramUser);
        Mono<SubscriptionContext> contextMono = stateService.getContext(telegramUser.getId(), SubscriptionContext.class);

        return Mono.zip(contextMono, userMono)
                .flatMap(this::persistSubscription);
    }

    /**
     * Устанавливает ID пользователя для подписки и сохраняет ее в репозиторий.
     *
     * @param tuple Кортеж, содержащий {@link SubscriptionContext} и {@link RecurixUser}.
     * @return {@code Mono} с сохраненной подпиской.
     */
    private Mono<Subscription> persistSubscription(Tuple2<SubscriptionContext, RecurixUser> tuple) {
        Subscription subscriptionToSave = tuple.getT1().getSubscription();
        RecurixUser owner = tuple.getT2();

        subscriptionToSave.setUserId(owner.id());

        log.info("Saving subscription '{}' for user {}", subscriptionToSave.getName(), owner.id());
        return subscriptionService.save(subscriptionToSave);
    }
}
