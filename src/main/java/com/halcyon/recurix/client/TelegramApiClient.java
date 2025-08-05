package com.halcyon.recurix.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TelegramApiClient {

    private final WebClient webClient;

    public TelegramApiClient(@Value("${telegram.bot.token}") String botToken) {
        this.webClient = WebClient.create("https://api.telegram.org/bot" + botToken);
    }

    public Mono<Void> deleteMessage(Long chatId, Integer messageId) {
        if (chatId == null || messageId == null) {
            return Mono.empty();
        }

        var deleteMessage = new DeleteMessage(chatId.toString(), messageId);

        return webClient.post()
                .uri("/deleteMessage")
                .bodyValue(deleteMessage)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorComplete(e -> true);
    }

    /**
     * Асинхронно отправляет всплывающее уведомление в ответ на нажатие инлайн-кнопки.
     * @param callbackQueryId Уникальный ID из объекта CallbackQuery.
     * @param text Текст уведомления (до 200 символов).
     * @return Mono<Void>, который завершается, когда запрос отправлен.
     */
    public Mono<Void> sendAnswerCallbackQuery(String callbackQueryId, String text) {
        var answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text(text)
                .showAlert(false)
                .build();

        return webClient.post()
                .uri("/answerCallbackQuery")
                .bodyValue(answer)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorComplete(e -> true);
    }
}
