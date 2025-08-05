package com.halcyon.recurix.support;

import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionMessageFactory {

    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public EditMessageText createEditMessage(
            Long userId,
            Integer messageId,
            Subscription subscription,
            InlineKeyboardMarkup keyboard
    ) {
        String summary = messageService.getMessage(
                "dialog.add.prompt.confirmation",
                subscription.getName(),
                subscription.getPrice(),
                subscription.getPaymentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                subscription.getCurrency(),
                subscription.getCategory()
        );

        return EditMessageText.builder()
                .chatId(userId)
                .messageId(messageId)
                .text(summary)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboard)
                .build();
    }

    /**
     * Формирует объект EditMessageText, содержащий отформатированный список подписок.
     * @param chatId ID пользователя/чата.
     * @param messageId ID сообщения, которое нужно отредактировать.
     * @param subscriptions Список подписок для отображения.
     * @return Готовый объект EditMessageText.
     */
    public EditMessageText createSubscriptionsListMessage(Long chatId, Integer messageId, List<Subscription> subscriptions) {
        return EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(formatSubscriptionList(subscriptions))
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboardService.getSubscriptionsKeyboard())
                .build();
    }

    public String formatSubscriptionList(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return messageService.getMessage("subscriptions.list.empty");
        }

        String list = subscriptions.stream()
                .map(subscription -> messageService.getMessage(
                        "subscriptions.list.item",
                        subscription.getName(),
                        subscription.getPrice(),
                        subscription.getCurrency(),
                        subscription.getPaymentDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.joining("\n"));

        return messageService.getMessage("subscriptions.list.header", list);
    }

    /**
     * Создает и возвращает Mono с сообщением об ошибке.
     *
     * @param userId    ID чата.
     * @param exception Исключение, содержащее код сообщения.
     * @return Mono с объектом SendMessage.
     */
    public Mono<SendMessage> createErrorMessage(Long userId, InvalidInputException exception) {
        SendMessage errorMessage = new SendMessage(userId.toString(), messageService.getMessage(exception.getMessageCode()));
        return Mono.just(errorMessage);
    }
}
