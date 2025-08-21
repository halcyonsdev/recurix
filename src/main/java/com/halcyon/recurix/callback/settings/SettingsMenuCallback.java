package com.halcyon.recurix.callback.settings;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.model.UserSettings;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.service.UserSettingsService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обрабатывает вход в меню настроек пользователя.
 * <p>
 * Срабатывает при нажатии на кнопку "Настройки" в главном меню.
 * Получает актуальные настройки пользователя и отображает их в виде интерактивного меню.
 *
 * @see ToggleRemindersCallback
 * @see ChangeReminderTimingCallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettingsMenuCallback implements Callback {

    private final UserService userService;
    private final UserSettingsService settingsService;
    private final KeyboardService keyboardService;
    private final LocalMessageService messageService;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.MENU_SETTINGS.equals(callbackData);
    }

    /**
     * Выполняет основную логику: получает настройки и отображает меню.
     *
     * @param update Объект {@link Update} от Telegram.
     * @return {@code Mono} с {@link EditMessageText} для отображения меню настроек.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        log.info("User {} entered settings menu.", query.getFrom().getId());

        return userService.findOrCreateUser(query.getFrom())
                .flatMap(settingsService::getSettings)
                .map(settings -> createSettingsMessage(query.getMessage(), settings));
    }

    /**
     * Создает или обновляет сообщение с меню настроек.
     * <p>
     * Этот публичный метод используется другими обработчиками для перерисовки
     * меню после того, как пользователь изменил какую-либо настройку.
     *
     * @param message  Сообщение, которое необходимо отредактировать.
     * @param settings Актуальные настройки пользователя для отображения.
     * @return Готовый объект {@link EditMessageText}.
     */
    public EditMessageText createSettingsMessage(MaybeInaccessibleMessage message, UserSettings settings) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(messageService.getMessage("settings.header"))
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboardService.getSettingsKeyboard(settings))
                .build();
    }
}
