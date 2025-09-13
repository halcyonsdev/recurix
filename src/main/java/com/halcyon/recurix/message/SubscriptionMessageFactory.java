package com.halcyon.recurix.message;

import com.halcyon.recurix.exception.InvalidInputException;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.Page;
import com.halcyon.recurix.support.PayloadEncoder;
import com.halcyon.recurix.support.PeriodFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionMessageFactory {

    private final LocalMessageService messageService;
    private final KeyboardService keyboardService;
    private final PayloadEncoder payloadEncoder;
    private final PeriodFormatter periodFormatter;

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral(" ")
            .appendPattern("MMM")
            .appendLiteral(" ")
            .appendValue(ChronoField.YEAR)
            .toFormatter(Locale.forLanguageTag("ru"));

    public EditMessageText createEditMessage(
                                             Long userId,
                                             Integer messageId,
                                             Subscription subscription,
                                             InlineKeyboardMarkup keyboard) {
        String summary = messageService.getMessage(
                "dialog.add.prompt.confirmation",
                subscription.getName(),
                subscription.getPrice(),
                subscription.getPaymentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                subscription.getCurrency(),
                subscription.getCategory(),
                periodFormatter.format(subscription.getRenewalMonths()));

        return EditMessageText.builder()
                .chatId(userId)
                .messageId(messageId)
                .text(summary)
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(keyboard)
                .build();
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

    /**
     * Создает новый объект SendMessage со списком подписок.
     * Подходит для отправки после удаления старого сообщения.
     * 
     * @param chatId    ID чата для отправки.
     * @param page      Объект страницы с подписками.
     * @param messageId ID нового сообщения, которое будет создано.
     * @return Готовый объект SendMessage.
     */
    public SendMessage createNewSubscriptionsPageMessage(Long chatId, Integer messageId, Page<Subscription> page) {
        var defaultContext = new SubscriptionListContext("paymentDate", Sort.Direction.ASC);

        return SendMessage.builder()
                .chatId(chatId)
                .text(formatSubscriptionsPage(page, messageId))
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getSubscriptionsPageKeyboard(page, defaultContext))
                .build();
    }

    public String formatSubscriptionsPage(Page<Subscription> page, Integer messageId) {
        if (page.totalElements() == 0) {
            return messageService.getMessage("subscriptions.list.empty");
        }

        String listItems = page.content().stream()
                .map(subscription -> messageService.getMessage(
                        "subscriptions.list.item",
                        subscription.getName(),
                        subscription.getPrice(),
                        subscription.getCurrency(),
                        subscription.getPaymentDate().format(DATE_FORMATTER),
                        "/view_" + payloadEncoder.encode(subscription.getId(), page.currentPage(), messageId)))
                .collect(Collectors.joining("\n\n"));

        String mainContent = messageService.getMessage(
                "subscriptions.list.paginated_header",
                listItems);

        return mainContent + "\n\n<code>────────────────────</code>";
    }

    /**
     * Формирует текстовое содержимое для детального просмотра одной подписки.
     * 
     * @param subscription Подписка для отображения.
     * @return Отформатированная строка для отправки.
     */
    public String formatSubscriptionDetail(Subscription subscription) {
        return messageService.getMessage(
                "subscription.detail.header",
                subscription.getName(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getPaymentDate().format(DATE_FORMATTER),
                subscription.getCategory(),
                periodFormatter.format(subscription.getRenewalMonths()));
    }
}
