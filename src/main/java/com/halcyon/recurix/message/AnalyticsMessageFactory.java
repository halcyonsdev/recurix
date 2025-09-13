package com.halcyon.recurix.message;

import com.halcyon.recurix.dto.AnalyticsDto;
import com.halcyon.recurix.dto.CategorySpendingDto;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnalyticsMessageFactory {

    private final LocalMessageService messageService;
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"));

    /**
     * Формирует полное текстовое сообщение для экрана аналитики.
     *
     * @param analyticsDto DTO с данными для отображения.
     * @param yearMonth    Выбранный месяц для заголовка.
     * @return Готовая строка для отправки в Telegram.
     */
    public String createAnalyticsMessage(AnalyticsDto analyticsDto, YearMonth yearMonth) {
        if (analyticsDto.monthlyTotal().compareTo(BigDecimal.ZERO) == 0 && analyticsDto.totalSubscriptions() == 0) {
            return messageService.getMessage("analytics.empty");
        }

        String title = yearMonth.format(MONTH_YEAR_FORMATTER);

        return messageService.getMessage("analytics.header", title) + "\n\n" +
                messageService.getMessage("analytics.total_subscriptions", analyticsDto.totalSubscriptions()) + "\n" +
                messageService.getMessage("analytics.monthly_total", analyticsDto.monthlyTotal()) + "\n\n" +
                buildCategoriesBlock(analyticsDto) + "\n\n" +
                buildMostExpensiveBlock(analyticsDto.mostExpensive()) + "\n\n" +
                buildNextPaymentBlock(analyticsDto.nextPayment());
    }

    private String buildCategoriesBlock(AnalyticsDto analyticsDto) {
        if (analyticsDto.spendingByCategory().isEmpty()) {
            return "";
        }

        String categoriesList = analyticsDto.spendingByCategory().stream()
                .map(category -> formatCategoryLine(category, analyticsDto.monthlyTotal()))
                .collect(Collectors.joining("\n"));

        return messageService.getMessage("analytics.categories_header") + "\n" + categoriesList;
    }

    private String formatCategoryLine(CategorySpendingDto categoryDto, BigDecimal monthlyTotal) {
        BigDecimal percentage = BigDecimal.ZERO;

        if (monthlyTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentage = categoryDto.total()
                    .multiply(new BigDecimal(100))
                    .divide(monthlyTotal, 0, RoundingMode.HALF_UP);
        }

        return messageService.getMessage("analytics.category_item",
                getEmojiForCategory(categoryDto.category()),
                categoryDto.category(),
                categoryDto.total(),
                percentage);
    }

    private String getEmojiForCategory(String category) {
        if (category == null) return "\uD83D\uDD39";

        return switch (category.toLowerCase()) {
            case "музыка" -> "🎵";
            case "стриминг", "кино" -> "🎬";
            case "образование" -> "📚";
            case "сервисы" -> "☁️";
            default -> "💸";
        };
    }

    private String buildMostExpensiveBlock(Subscription subscription) {
        if (subscription == null) return "";

        return messageService.getMessage("analytics.most_expensive",
                subscription.getName(),
                subscription.getPrice());
    }

    private String buildNextPaymentBlock(Subscription subscription) {
        if (subscription == null) return "";

        // TODO: Реализовать более умный расчет "через N дней"
        return messageService.getMessage("analytics.next_payment",
                subscription.getName(),
                subscription.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
