package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.KeyboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Обработчик нажатия на конкретную дату в инлайн-календаре.
 * <p>
 * Этот callback перерисовывает клавиатуру календаря,
 * подсвечивая выбранную пользователем дату и делая кнопку "Применить" активной.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarDateSelectionCallback implements Callback {

    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_SELECT_PREFIX);
    }

    /**
     * Перерисовывает календарь с выделенной датой.
     * <p>
     * Метод парсит дату из {@code callbackData}, а затем использует {@link KeyboardService}
     * для генерации новой инлайн-клавиатуры, где эта дата будет отмечена как выбранная.
     *
     * @param update Входящий объект {@link Update} от Telegram.
     * @return {@code Mono} с объектом {@link EditMessageReplyMarkup}, содержащим только
     *         обновленную клавиатуру
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData().substring(CallbackData.CALENDAR_SELECT_PREFIX.length());
        String[] parts = data.split("_");
        LocalDate selectedDate = LocalDate.parse(parts[0]);
        String backCallbackData = parts[1];

        log.info("User {} pre-selected date: {}", callbackQuery.getFrom().getId(), selectedDate);

        return Mono.fromCallable(() -> EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(keyboardService.getCalendarKeyboard(YearMonth.from(selectedDate), selectedDate, backCallbackData))
                .build());
    }
}
