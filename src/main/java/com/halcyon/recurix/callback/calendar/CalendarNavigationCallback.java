package com.halcyon.recurix.callback.calendar;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.service.KeyboardService;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

/**
 * Обработчик нажатий на кнопки навигации ("←", "→") в инлайн-календаре.
 * <p>
 * Отвечает за перерисовку клавиатуры календаря на предыдущий или следующий месяц,
 * сохраняя при этом информацию о ранее выбранной дате, если она была.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarNavigationCallback implements Callback {

    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_NAV_PREFIX);
    }

    /**
     * Перерисовывает календарь на новый месяц.
     * <p>
     * Метод парсит целевой месяц и ранее выбранную дату из {@code callbackData}.
     * Затем он генерирует и отправляет новую клавиатуру, не меняя текст сообщения.
     *
     * @param update Входящий объект {@link Update} от Telegram.
     * @return {@code Mono} с объектом {@link EditMessageReplyMarkup}, содержащим только
     *             обновленную клавиатуру
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        String data = callbackData.substring(CallbackData.CALENDAR_NAV_PREFIX.length());
        String[] parts = data.split("_");

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid calendar navigation callback data structure: " + callbackData);
        }

        YearMonth targetYearMonth = YearMonth.parse(parts[0]);
        LocalDate selectedDate = parts[1].isEmpty()
                ? null
                : LocalDate.parse(parts[1]);
        String backCallbackData = parts[2];

        log.info("User {} navigating calendar to {}. Preserving selected date [{}] and back command [{}]",
                callbackQuery.getFrom().getId(), targetYearMonth, selectedDate, backCallbackData);

        return Mono.fromCallable(() -> EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(keyboardService.getCalendarKeyboard(targetYearMonth, selectedDate, backCallbackData))
                .build());
    }
}
