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
 * Обработчик нажатий на кнопки быстрого выбора периода (+1 месяц, +6 месяцев, +1 год).
 * <p>
 * Этот callback рассчитывает новую дату, отталкиваясь либо от уже выбранной пользователем
 * даты, либо от текущей, а затем перерисовывает календарь с новой выбранной датой.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarQuickSelectCallback implements Callback {

    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith(CallbackData.CALENDAR_QUICK_PREFIX);
    }

    /**
     * Рассчитывает и устанавливает новую дату, затем перерисовывает календарь.
     * <p>
     * Логика определения базовой даты:
     * <ul>
     *     <li>Если пользователь уже выбрал дату в календаре, она используется как точка отсчета.</li>
     *     <li>Если дата не была выбрана, точкой отсчета становится {@link LocalDate#now()}.</li>
     * </ul>
     *
     * @param update Входящий объект {@link Update} от Telegram.
     * @return {@code Mono} с объектом {@link EditMessageReplyMarkup}, содержащим только
     *         обновленную клавиатуру
     */
    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        String data = callbackData.substring(CallbackData.CALENDAR_QUICK_PREFIX.length());
        String[] parts = data.split("_");

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid calendar quick select callback data structure: " + callbackData);
        }

        String period = parts[0];
        LocalDate baseDate = parts[1].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[1]);
        String backCallbackData = parts[2];

        LocalDate newSelectedDate = switch (period) {
            case "1m" -> baseDate.plusMonths(1);
            case "6m" -> baseDate.plusMonths(6);
            case "1y" -> baseDate.plusYears(1);
            default -> baseDate;
        };

        log.info("User {} used quick select '{}' from base date {}. New date is {}. Back command is [{}]",
                callbackQuery.getFrom().getId(), period, baseDate, newSelectedDate, backCallbackData);

        return Mono.fromCallable(() -> EditMessageReplyMarkup.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .replyMarkup(keyboardService.getCalendarKeyboard(YearMonth.from(newSelectedDate), newSelectedDate, backCallbackData))
                .build());
    }
}
