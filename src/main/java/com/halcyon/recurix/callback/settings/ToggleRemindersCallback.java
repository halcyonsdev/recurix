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
 * Обрабатывает переключение статуса напоминаний (Вкл/Выкл).
 * <p>
 * Срабатывает при нажатии на соответствующую кнопку в меню настроек.
 * Инвертирует текущее значение настройки, сохраняет его в базе данных,
 * отправляет пользователю всплывающее уведомление и перерисовывает
 * меню настроек с обновленным статусом.
 *
 * @see SettingsMenuCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToggleRemindersCallback implements Callback {

    private final UserService userService;
    private final UserSettingsService settingsService;
    private final SettingsMenuCallback settingsMenuCallback;
    private final TelegramApiClient telegramApiClient;
    private final LocalMessageService messageService;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.SETTINGS_TOGGLE_REMINDERS.equals(callbackData);
    }

    /**
     * Запускает процесс переключения статуса напоминаний.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для обновления меню настроек.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery query = update.getCallbackQuery();

        return userService.findOrCreateUser(query.getFrom())
                .flatMap(this::toggleAndSaveSettings)
                .flatMap(savedSettings -> sendNotificationAndRefreshMenu(query, savedSettings));
    }

    /**
     * Получает текущие настройки, инвертирует статус напоминаний и сохраняет изменения.
     *
     * @param user Пользователь системы.
     * @return {@code Mono} с обновленными и сохраненными настройками.
     */
    private Mono<UserSettings> toggleAndSaveSettings(RecurixUser user) {
        return settingsService.getSettings(user)
                .flatMap(settings -> {
                    boolean newStatus = !settings.isRemindersEnabled();

                    log.info("User {} toggled reminders to {}", user.telegramId(), newStatus);

                    settings.setRemindersEnabled(newStatus);
                    return settingsService.save(settings);
                });
    }

    /**
     * Отправляет всплывающее уведомление пользователю и возвращает обновленное меню настроек.
     *
     * @param query    Исходный {@link CallbackQuery}.
     * @param settings Сохраненные настройки.
     * @return {@code Mono} с {@link EditMessageText}.
     */
    private Mono<BotApiMethod<? extends Serializable>> sendNotificationAndRefreshMenu(CallbackQuery query,
                                                                                      UserSettings settings) {
        String messageCode = settings.isRemindersEnabled()
                ? "settings.changed.reminders_on"
                : "settings.changed.reminders_off";
        String notificationText = messageService.getMessage(messageCode);

        return telegramApiClient.sendAnswerCallbackQuery(query.getId(), notificationText)
                .thenReturn(settingsMenuCallback.createSettingsMessage(query.getMessage(), settings));
    }
}
