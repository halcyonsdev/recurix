package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик для "неактивных" или информационных кнопок календаря.
 * <p>
 * Этот callback извлекает тип уведомления из {@code callbackData}
 * и отправляет пользователю соответствующее всплывающее уведомление (Callback Answer).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarNotificationCallback implements Callback {

    private final LocalMessageService messageService;
    private final TelegramApiClient telegramApiClient;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_NOTIFY_PREFIX);
    }

    /**
     * Отправляет пользователю всплывающее уведомление.
     * <p>
     * Метод определяет текст уведомления на основе данных из callback-запроса
     * и использует {@link TelegramApiClient} для его отправки.
     *
     * @param update Входящий объект {@link Update} от Telegram.
     * @return Всегда возвращает {@code Mono.empty()}, так как никакие сообщения
     *         в чате изменять не нужно.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String notificationType = callbackQuery.getData().substring(CallbackData.CALENDAR_NOTIFY_PREFIX.length());

        String notificationText = switch (notificationType) {
            case "past_date" -> messageService.getMessage("error.date.in_past");
            case "no_date_selected" -> messageService.getMessage("error.date.no_selected");
            default -> messageService.getMessage("error.unsupported.action");
        };

        log.debug("User {} triggered a notification callback: {}", callbackQuery.getFrom().getId(), notificationType);

        return telegramApiClient.sendAnswerCallbackQuery(callbackQuery.getId(), notificationText, true)
                .then(Mono.empty());
    }
}
