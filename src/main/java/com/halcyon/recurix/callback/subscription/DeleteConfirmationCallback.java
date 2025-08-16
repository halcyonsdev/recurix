package com.halcyon.recurix.callback.subscription;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.SubscriptionService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает нажатие кнопки "Удалить" в детальном просмотре подписки.
 * <p>
 * Этот класс является промежуточным шагом в процессе удаления. Его основная
 * задача — показать пользователю диалог с уточнением, действительно ли он
 * хочет удалить подписку, чтобы предотвратить случайные действия.
 *
 * @see DeleteExecuteCallback
 * @see SubscriptionViewCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteConfirmationCallback implements Callback {

    private final SubscriptionService subscriptionService;
    private final KeyboardService keyboardService;
    private final LocalMessageService messageService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SUB_DELETE_CONFIRM_PREFIX);
    }

    /**
     * Находит подписку и заменяет текущее сообщение
     * на диалог подтверждения удаления с кнопками "Да, удалить" и "Нет, назад".
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для обновления сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData().substring(CallbackData.SUB_DELETE_CONFIRM_PREFIX.length());
        String[] parts = data.split("_");
        Long subscriptionId = Long.parseLong(parts[0]);
        int pageNumber = Integer.parseInt(parts[1]);

        log.info("User {} requested deletion confirmation for subscription {}", callbackQuery.getFrom().getId(), subscriptionId);

        return subscriptionService.findById(subscriptionId)
                .map(subscription -> EditMessageText.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .text(messageService.getMessage("dialog.delete.confirm.prompt", subscription.getName()))
                        .parseMode(ParseMode.MARKDOWN)
                        .replyMarkup(keyboardService.getDeleteConfirmationKeyboard(subscriptionId, pageNumber))
                        .build());
    }
}
