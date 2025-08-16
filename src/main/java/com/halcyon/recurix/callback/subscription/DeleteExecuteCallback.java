package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.Page;
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
 * Обрабатывает финальное подтверждение на удаление подписки.
 * <p>
 * Этот класс является завершающим шагом в процессе удаления. Он выполняет
 * следующие действия:
 * <ol>
 * <li>Отправляет пользователю всплывающее уведомление об успешном удалении.</li>
 * <li>Находит внутреннего пользователя системы по его Telegram ID.</li>
 * <li>Удаляет подписку из базы данных.</li>
 * <li>Пересчитывает корректный номер страницы для отображения (на случай, если удаленная
 * подписка была последней на странице).</li>
 * <li>Обновляет сообщение, показывая актуализированный список подписок.</li>
 * </ol>
 *
 * @see DeleteConfirmationCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteExecuteCallback implements Callback {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final SubscriptionMessageFactory messageFactory;
    private final TelegramApiClient telegramApiClient;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_DELETE_EXECUTE_PREFIX);
    }

    /**
     * Запускает процесс удаления подписки и обновления списка.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono}, содержащий {@link EditMessageText} с обновленным списком.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        CallbackContext context = parseCallbackContext(query.getData());
        User telegramUser = query.getFrom();

        log.info("User {} confirmed deletion for subscription {}. Was on page {}",
                telegramUser.getId(), context.subscriptionId(), context.originalPageNumber());

        return sendSuccessNotification(query.getId())
                .then(userService.findOrCreateUser(telegramUser)
                        .flatMap(recurixUser -> deleteAndRefreshList(query, context, recurixUser)));
    }

    /**
     * Отправляет всплывающее уведомление об успешном удалении.
     *
     * @param callbackQueryId ID исходного запроса.
     * @return {@code Mono<Void>}, завершающийся после отправки запроса.
     */
    private Mono<Void> sendSuccessNotification(String callbackQueryId) {
        return telegramApiClient.sendAnswerCallbackQuery(
                callbackQueryId,
                messageService.getMessage("delete.success"));
    }

    /**
     * Извлекает ID подписки и номер страницы из строки callback-данных.
     *
     * @param callbackData Cтрока данных.
     * @return Объект {@link CallbackContext} с извлеченными данными.
     */
    private CallbackContext parseCallbackContext(String callbackData) {
        String data = callbackData.substring(CallbackData.SUB_DELETE_EXECUTE_PREFIX.length());
        String[] parts = data.split("_");
        long subscriptionId = Long.parseLong(parts[0]);
        int pageNumber = Integer.parseInt(parts[1]);

        return new CallbackContext(subscriptionId, pageNumber);
    }

    /**
     * Удаляет подписку и возвращает {@link EditMessageText} с обновленным списком.
     *
     * @param query       Исходный {@link CallbackQuery}.
     * @param context     Данные для выполнения операции.
     * @param recurixUser Внутренний пользователь системы.
     * @return {@code Mono} с готовым для отправки сообщением.
     */
    private Mono<BotApiMethod<? extends Serializable>>
            deleteAndRefreshList(CallbackQuery query, CallbackContext context, RecurixUser recurixUser) {
        return subscriptionService.deleteById(context.subscriptionId)
                .then(subscriptionService.countByUserId(recurixUser.id()))
                .flatMap(totalCount -> {
                    int targetPage = calculateTargetPage(totalCount, context.originalPageNumber);
                    Pageable pageable = PageRequest.of(targetPage, PaginationConstants.DEFAULT_PAGE_SIZE,
                            PaginationConstants.DEFAULT_SORT);
                    return subscriptionService.getSubscriptionsAsPage(recurixUser.id(), pageable);
                })
                .map(page -> createListPageMessage(query, page));
    }

    /**
     * Вычисляет корректный номер страницы для отображения после удаления элемента.
     *
     * @param totalCount         Общее количество элементов после удаления.
     * @param originalPageNumber Страница, на которой находился пользователь.
     * @return Номер страницы, которую следует показать пользователю.
     */
    private int calculateTargetPage(int totalCount, int originalPageNumber) {
        int pageSize = PaginationConstants.DEFAULT_PAGE_SIZE;
        int newTotalPages = (totalCount == 0)
                ? 1
                : (int) Math.ceil((double) totalCount / pageSize);
        int targetPage = Math.min(originalPageNumber, newTotalPages - 1);

        return Math.max(targetPage, 0);
    }

    /**
     * Создает ответное сообщение со страницей подписок.
     *
     * @param query Исходный {@link CallbackQuery}.
     * @param page  Страница с подписками для отображения.
     * @return Готовый объект {@link EditMessageText}.
     */
    private EditMessageText createListPageMessage(CallbackQuery query, Page<Subscription> page) {
        var defaultContext = new SubscriptionListContext("paymentDate", Sort.Direction.ASC);

        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text(messageFactory.formatSubscriptionsPage(page, query.getMessage().getMessageId()))
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getSubscriptionsPageKeyboard(page, defaultContext))
                .build();
    }

    /**
     * Внутренний record для хранения данных из callback-запроса.
     */
    private record CallbackContext(long subscriptionId, int originalPageNumber) {}
}
