package com.halcyon.recurix;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Утилитарный класс-строитель для создания тестовых объектов Telegram.
 */
public class TestDataBuilder {

    public static final Long DEFAULT_USER_ID = 12345L;
    public static final Long DEFAULT_CHAT_ID = 54321L;
    public static final Integer DEFAULT_MESSAGE_ID = 987;
    public static final String DEFAULT_FIRST_NAME = "Tester";

    public static Update buildUpdateWithMessage(String text) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();

        chat.setId(DEFAULT_CHAT_ID);
        message.setChat(chat);
        message.setMessageId(DEFAULT_MESSAGE_ID);
        message.setText(text);
        message.setFrom(buildTelegramUser());

        update.setMessage(message);
        return update;
    }

    public static User buildTelegramUser() {
        User telegramUser = new User();
        telegramUser.setId(DEFAULT_USER_ID);
        telegramUser.setFirstName(DEFAULT_FIRST_NAME);
        telegramUser.setIsBot(false);
        return telegramUser;
    }
}
