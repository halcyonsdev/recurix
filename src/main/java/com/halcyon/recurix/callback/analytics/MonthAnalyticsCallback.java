package com.halcyon.recurix.callback.analytics;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import com.halcyon.recurix.dto.AnalyticsDto;
import com.halcyon.recurix.message.AnalyticsMessageFactory;
import com.halcyon.recurix.service.AnalyticsService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthAnalyticsCallback implements Callback {

    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final AnalyticsMessageFactory messageFactory;
    private final KeyboardService keyboardService;

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && (callbackData.equals(CallbackData.MENU_ANALYTICS) || callbackData.startsWith(CallbackData.ANALYTICS_NAV_PREFIX));
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        YearMonth targetMonth = (callbackData.startsWith(CallbackData.ANALYTICS_NAV_PREFIX))
                ? YearMonth.parse(callbackData.substring(CallbackData.ANALYTICS_NAV_PREFIX.length()))
                : YearMonth.now();

        log.info("User {} requested analytics for {}", update.getCallbackQuery().getFrom().getId(), targetMonth);

        return userService.findOrCreateUser(update.getCallbackQuery().getFrom())
                .flatMap(user -> analyticsService.getAnalyticsForMonth(user, targetMonth))
                .map(analyticsDto -> createAnalyticsMessage(update, analyticsDto, targetMonth));
    }

    private EditMessageText createAnalyticsMessage(Update update, AnalyticsDto analyticsDto, YearMonth yearMonth) {
        return EditMessageText.builder()
                .chatId(update.getCallbackQuery().getMessage().getChatId())
                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                .text(messageFactory.createAnalyticsMessage(analyticsDto, yearMonth))
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboardService.getAnalyticsKeyboard(yearMonth))
                .build();
    }
}
