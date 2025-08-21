package com.halcyon.recurix.callback.settings;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.client.TelegramApiClient;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.UserSettings;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.UserSettingsService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает изменение времени напоминания в меню настроек.
 * <p>
 * Срабатывает при нажатии на кнопки "За 1 дн.", "За 3 дн." и т.д.
 * Обновляет соответствующее поле в настройках пользователя, отправляет
 * всплывающее уведомление и перерисовывает меню настроек с актуальными данными.
 *
 * @see SettingsMenuCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChangeReminderTimingCallback implements Callback {

    private final UserService userService;
    private final UserSettingsService settingsService;
    private final TelegramApiClient telegramApiClient;
    private final LocalMessageService messageService;
    private final SettingsMenuCallback settingsMenuCallback;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.SETTINGS_CHANGE_DAYS_PREFIX);
    }

    /**
     * Запускает процесс обновления настройки времени напоминания.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для обновления меню настроек,
     *             или {@code Mono.empty()} если изменение не требуется.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        int requestedDats = parseDays(query.getData());

        return userService.findOrCreateUser(query.getFrom())
                .flatMap(user -> updateSettingsIfNeeded(user, requestedDats))
                .flatMap(result -> {
                    if (!result.wasUpdated()) {
                        return Mono.empty();
                    }

                    return sendNotificationAndRefreshMenu(query, result.settings());
                });
    }

    /**
     * Извлекает количество дней из строки callback-данных.
     *
     * @param callbackData Строка данных.
     * @return Количество дней.
     */
    private int parseDays(String callbackData) {
        return Integer.parseInt(callbackData.substring(CallbackData.SETTINGS_CHANGE_DAYS_PREFIX.length()));
    }

    /**
     * Обновляет настройку, если новое значение отличается от текущего.
     *
     * @param user          Пользователь системы.
     * @param requestedDays Новое количество дней для напоминания.
     * @return {@code Mono} с результатом обновления.
     */
    private Mono<UpdateResult> updateSettingsIfNeeded(RecurixUser user, int requestedDays) {
        return settingsService.getSettings(user)
                .flatMap(settings -> {
                    if (settings.getReminderDaysBefore() == requestedDays) {
                        log.debug("User {} selected the same reminder timing ({} days), no update needed.", user.telegramId(),
                                requestedDays);
                        return Mono.just(new UpdateResult(settings, false));
                    }

                    log.info("User {} changing reminder timing to {} days.", user.telegramId(), requestedDays);
                    settings.setReminderDaysBefore(requestedDays);

                    return settingsService.save(settings)
                            .map(savedSettings -> new UpdateResult(savedSettings, true));
                });
    }

    /**
     * Отправляет всплывающее уведомление и возвращает обновленное меню настроек.
     *
     * @param query    Исходный {@link CallbackQuery}.
     * @param settings Сохраненные настройки.
     * @return {@code Mono} с {@link EditMessageText}.
     */
    private Mono<BotApiMethod<? extends Serializable>> sendNotificationAndRefreshMenu(CallbackQuery query,
                                                                                      UserSettings settings) {
        String notificationText = messageService.getMessage("settings.changed.days_set", settings.getReminderDaysBefore());
        return telegramApiClient.sendAnswerCallbackQuery(
                query.getId(),
                notificationText).thenReturn(settingsMenuCallback.createSettingsMessage(query.getMessage(), settings));
    }

    /**
     * Внутренний record для передачи результата обновления между шагами.
     */
    private record UpdateResult(UserSettings settings, boolean wasUpdated) {}
}
