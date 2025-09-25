package com.halcyon.recurix.callback.analytics;

import com.halcyon.recurix.callback.Callback;
import com.halcyon.recurix.callback.CallbackData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
@Slf4j
public class YearAnalyticsCallback implements Callback {
    @Override
    public boolean supports(String callbackData) {
        return CallbackData.ANALYTICS_BY_YEAR.equals(callbackData);
    }

    @Override
    public Mono<BotApiMethod<? extends Serializable>> execute(Update update) {
        return null;
    }
}
