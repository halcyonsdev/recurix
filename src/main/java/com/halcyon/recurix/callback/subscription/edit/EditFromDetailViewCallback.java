package com.halcyon.recurix.callback.subscription.edit;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.callback.subscription.BackToDetailViewCallback;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.context.SubscriptionContext;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает запрос на начало редактирования подписки из ее детального просмотра.
 * <p>
 * Инициализирует диалог редактирования в Redis и заменяет клавиатуру сообщения
 * на меню с полями для редактирования.
 *
 * @see BackToDetailViewCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EditFromDetailViewCallback implements Callback {

    private final SubscriptionService subscriptionService;
    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_EDIT_DETAIL_PREFIX);
    }

    /**
     * Выполняет основную логику: инициализирует контекст и обновляет клавиатуру.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageReplyMarkup} для обновления клавиатуры.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        CallbackContext context = parseCallbackContext(query.getData());

        log.info("User {} initiates editing for subscription {}. Was on page {}",
                query.getFrom().getId(), context.subscriptionId(), context.pageNumber());

        return subscriptionService.findById(context.subscriptionId)
                .flatMap(subscription -> initializeConversation(query, context, subscription))
                .map(subscription -> createEditMenuMarkup(query, context));
    }

    /**
     * Извлекает ID подписки и номер страницы из строки callback-данных.
     *
     * @param callbackData Cтрока данных.
     * @return Объект {@link CallbackContext} с извлеченными данными.
     * @throws IllegalArgumentException если данные имеют неверный формат.
     */
    private CallbackContext parseCallbackContext(String callbackData) {
        String data = callbackData.substring(CallbackData.SUB_EDIT_DETAIL_PREFIX.length());
        String[] parts = data.split("_");
        Long subscriptionId = Long.parseLong(parts[0]);
        int pageNumber = Integer.parseInt(parts[1]);

        return new CallbackContext(subscriptionId, pageNumber);
    }

    /**
     * Сохраняет контекст диалога (редактируемую подписку, ID сообщения) в Redis.
     *
     * @param query        Исходный {@link CallbackQuery}.
     * @param context      Распарсенные данные из callback-запроса.
     * @param subscription Объект подписки для редактирования.
     * @return {@code Mono} с объектом подписки для дальнейшего использования.
     */
    private Mono<Subscription> initializeConversation(CallbackQuery query, CallbackContext context, Subscription subscription) {
        var subscriptionContext = new SubscriptionContext(subscription, query.getMessage().getMessageId(), context.pageNumber);
        return stateService.setContext(query.getFrom().getId(), subscriptionContext).thenReturn(subscription);
    }

    /**
     * Создает ответ для Telegram API с обновленной клавиатурой.
     *
     * @param query   Исходный {@link CallbackQuery}.
     * @param context Распарсенные данные из callback-запроса.
     * @return Готовый объект {@link EditMessageReplyMarkup}.
     */
    private EditMessageReplyMarkup createEditMenuMarkup(CallbackQuery query, CallbackContext context) {
        String backCallbackData = CallbackData.SUB_BACK_TO_VIEW_PREFIX + context.subscriptionId + "_" + context.pageNumber;
        InlineKeyboardMarkup editKeyboard = keyboardService.getEditKeyboard(backCallbackData);

        return EditMessageReplyMarkup.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .replyMarkup(editKeyboard)
                .build();
    }

    /**
     * Внутренний record для хранения данных из callback-запроса.
     */
    private record CallbackContext(Long subscriptionId, int pageNumber) {}
}
