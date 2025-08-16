package com.halcyon.recurix.command;

import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.UserService;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import reactor.core.publisher.Mono;

/**
 * Обработчик стартовой команды /start.
 * <p>
 * Эта команда является точкой входа для новых пользователей. Она регистрирует
 * пользователя в системе (если он новый) и отправляет приветственное сообщение
 * с главным меню.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartCommand implements BotCommand {

    private final UserService userService;
    private final ConversationStateService stateService;
    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;

    private static final String START_COMMAND = "/start";

    @Override
    public boolean supports(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().getText().startsWith(START_COMMAND);
    }

    /**
     * Выполняет логику команды /start.
     * <p>
     * Метод выполняет следующие ключевые действия:
     * <ol>
     * <li><b>Сбрасывает диалог:</b> Полностью очищает любое текущее состояние диалога пользователя в
     * Redis.</li>
     * <li><b>Регистрирует пользователя:</b> Находит или создает пользователя в базе данных.</li>
     * <li><b>Отправляет приветствие:</b> Формирует и отправляет приветственное сообщение с главной
     * клавиатурой.</li>
     * </ol>
     *
     * @param update Объект с сообщением от пользователя.
     * @return {@code Mono} с объектом {@link SendMessage} для отправки пользователю.
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        User telegramUser = update.getMessage().getFrom();

        log.info("Executing /start command for user: {} (ID: {})", telegramUser.getFirstName(), telegramUser.getId());

        return stateService.clearState(telegramUser.getId())
                .then(userService.findOrCreateUser(telegramUser)
                        .map(user -> {
                            String welcomeText = messageService.getMessage("welcome.message");

                            return SendMessage.builder()
                                    .chatId(update.getMessage().getChatId())
                                    .text(welcomeText)
                                    .parseMode(ParseMode.MARKDOWN)
                                    .replyMarkup(keyboardService.getMainMenuKeyboard())
                                    .build();
                        }));
    }
}
