package com.halcyon.recurix.callback.main;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Обработчик callback-запроса для возврата в главное меню.
 * <p>
 * Срабатывает при нажатии на кнопки "Меню", "Отмена" или другие,
 * предполагающие завершение текущего действия. Его основная задача —
 * прервать любой активный диалог, очистив состояние пользователя, и показать
 * стартовое сообщение с главным меню.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuCallback implements Callback {

    private final UserService userService;
    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return CallbackData.MENU.equals(callbackData);
    }

    /**
     * Завершает текущий диалог и возвращает пользователя в главное меню.
     * <p>
     * Метод выполняет следующие действия:
     * <ol>
     *     <li>Очищает состояние (state) диалога пользователя в Redis.</li>
     *     <li>Находит пользователя в базе данных для получения его имени.</li>
     *     <li>Формирует и возвращает приветственное сообщение с клавиатурой главного меню.</li>
     * </ol>
     *
     * @param update Объект, содержащий callback-запрос от пользователя.
     * @return {@code Mono} с объектом {@link EditMessageText} для обновления исходного сообщения.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        User  telegramUser = callbackQuery.getFrom();
        Long userId = telegramUser.getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("User {} returned to the main menu, clearing state/context.", userId);

        return stateService.clearState(userId)
                .then(userService.findOrCreateUser(telegramUser)
                                .map(user -> EditMessageText.builder()
                                        .chatId(userId)
                                        .messageId(messageId)
                                        .text(messageService.getMessage("welcome.message", user.firstName()))
                                        .parseMode(ParseMode.MARKDOWN)
                                        .replyMarkup(keyboardService.getMainMenuKeyboard())
                                        .build())
                );
    }
}
