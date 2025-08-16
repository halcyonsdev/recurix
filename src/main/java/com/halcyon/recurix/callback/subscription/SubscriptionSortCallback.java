package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.PaginationConstants;
import com.halcyon.recurix.support.SubscriptionMessageFactory;
import java.io.Serializable;
import java.util.Objects;
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
 * Обрабатывает запросы на сортировку списка подписок.
 * <p>
 * Срабатывает при нажатии на кнопки сортировки. Класс вычисляет и сохраняет
 * новое состояние сортировки, а затем обновляет сообщение с отсортированным списком.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionSortCallback implements Callback {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final ConversationStateService stateService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory messageFactory;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_SORT_PREFIX);
    }

    /**
     * Применяет новые параметры сортировки к списку подписок и обновляет сообщение.
     * <p>
     * Метод является точкой входа, которая оркестрирует весь процесс:
     * парсинг запроса, обновление состояния и формирование финального ответа.
     *
     * @param update Объект {@link Update} от Telegram, содержащий {@link CallbackQuery}.
     * @return {@code Mono}, содержащий {@link EditMessageText} для обновления исходного сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User telegramUser = callbackQuery.getFrom();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        SortRequest sortRequest = parseSortRequest(callbackQuery.getData());

        return updateAndGetSortContext(telegramUser.getId(), sortRequest.requestedSortField)
                .flatMap(newContext -> buildSuccessResponse(
                        telegramUser,
                        messageId,
                        sortRequest.currentPage,
                        newContext));
    }

    /**
     * Извлекает и разбирает данные для сортировки из строки callback-запроса.
     *
     * @param callbackData Строка данных из {@link CallbackQuery}.
     * @return Объект {@link SortRequest}, содержащий поле для сортировки и номер текущей страницы.
     */
    private SortRequest parseSortRequest(String callbackData) {
        String data = callbackData.substring(CallbackData.SUB_SORT_PREFIX.length());
        String[] parts = data.split("_");

        String requestedSortField = parts[0];
        int currentPage = Integer.parseInt(parts[1]);

        return new SortRequest(requestedSortField, currentPage);
    }

    /**
     * Асинхронно обновляет и возвращает контекст сортировки для пользователя.
     * <p>
     * Метод загружает текущий контекст из Redis, вычисляет новый на основе запроса пользователя,
     * сохраняет его обратно в Redis и возвращает в виде {@code Mono}.
     *
     * @param userId             ID пользователя, для которого обновляется контекст.
     * @param requestedSortField Поле, по которому пользователь запросил сортировку.
     * @return {@code Mono}, который эммитит новый сохраненный {@link SubscriptionListContext}.
     */
    private Mono<SubscriptionListContext> updateAndGetSortContext(Long userId, String requestedSortField) {
        return stateService.getListContext(userId)
                .defaultIfEmpty(new SubscriptionListContext("paymentDate", Sort.Direction.DESC))
                .flatMap(currentContext -> {
                    SubscriptionListContext newContext = calculateNextContext(currentContext, requestedSortField);
                    log.info("User {} changed sort context to: {}", userId, newContext);

                    return stateService.setListContext(userId, newContext).thenReturn(newContext);
                });
    }

    /**
     * Вычисляет следующий контекст сортировки на основе текущего состояния и запрошенного поля.
     *
     * @param currentContext     Текущий контекст с полем и направлением сортировки.
     * @param requestedSortField Поле, по которому пользователь запросил сортировку.
     * @return Новый, вычисленный контекст сортировки.
     */
    private SubscriptionListContext calculateNextContext(SubscriptionListContext currentContext, String requestedSortField) {
        if (Objects.equals(currentContext.sortField(), requestedSortField)) {
            Sort.Direction newDirection = currentContext.sortDirection() == Sort.Direction.ASC
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            return new SubscriptionListContext(requestedSortField, newDirection);
        }

        return new SubscriptionListContext(requestedSortField, Sort.Direction.ASC);
    }

    /**
     * Формирует финальный ответ для пользователя в виде {@link EditMessageText}.
     * <p>
     * Метод создает {@link Pageable} на основе нового контекста сортировки, запрашивает
     * данные из сервиса подписок и строит готовое сообщение с обновленным списком и клавиатурой.
     *
     * @param telegramUser пользователь для отправки сообщения.
     * @param messageId    ID сообщения, которое нужно отредактировать.
     * @param currentPage  Номер страницы, которую нужно отобразить.
     * @param newContext   Новый контекст сортировки для запроса данных и отрисовки клавиатуры.
     * @return {@code Mono}, содержащий готовый к отправке объект {@link EditMessageText}.
     */
    private Mono<EditMessageText> buildSuccessResponse(
                                                       User telegramUser,
                                                       Integer messageId,
                                                       int currentPage,
                                                       SubscriptionListContext newContext) {
        Pageable pageable = PageRequest.of(
                currentPage,
                PaginationConstants.DEFAULT_PAGE_SIZE,
                Sort.by(newContext.sortDirection(), newContext.sortField()));

        return userService.findOrCreateUser(telegramUser)
                .flatMap(user -> subscriptionService.getSubscriptionsAsPage(user.id(), pageable))
                .map(page -> EditMessageText.builder()
                        .chatId(telegramUser.getId())
                        .messageId(messageId)
                        .text(messageFactory.formatSubscriptionsPage(page, messageId))
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(keyboardService.getSubscriptionsPageKeyboard(page, newContext))
                        .build());
    }

    /**
     * Простая запись (record) для хранения разобранных данных из callback-запроса.
     * 
     * @param requestedSortField Поле, по которому запрошена сортировка ("paymentDate" или "price").
     * @param currentPage        Текущий номер страницы, на которой находится пользователь.
     */
    private record SortRequest(String requestedSortField, int currentPage) {}
}
