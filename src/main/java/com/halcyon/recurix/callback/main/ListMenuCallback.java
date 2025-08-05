package com.halcyon.recurix.callback.main;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

import java.io.Serializable;

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
     *     <li>Извлекает информацию о пользователе, чате и сообщении из объекта {@code Update}.</li>
     *     <li>Находит или создает пользователя в базе данных.</li>
     *     <li>Получает все подписки этого пользователя.</li>
     *     <li>Использует {@link SubscriptionMessageFactory} для создания отформатированного сообщения со списком.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText},
     * который обновляет исходное сообщение, отображая список подписок.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        User telegramUser = update.getCallbackQuery().getFrom();
        Long userId = telegramUser.getId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        log.info("User {} requested their subscription list.", userId);

        return userService.findOrCreateUser(telegramUser)
                .flatMap(user -> subscriptionService.getAllByUserId(user.id()).collectList())
                .map(subscriptions -> subscriptionMessageFactory.createSubscriptionsListMessage(
                        userId,
                        messageId,
                        subscriptions
                ));
    }
}
