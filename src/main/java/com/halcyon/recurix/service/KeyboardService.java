package com.halcyon.recurix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.halcyon.recurix.callback.CallbackData.*;

/**
 * Сервис для создания и управления инлайн-клавиатурами.
 */
@Service
@RequiredArgsConstructor
public class KeyboardService {

    private final LocalMessageService messageService;

    public InlineKeyboardMarkup getMainMenuKeyboard() {
        var addButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button.add"))
                .callbackData(MENU_ADD_SUBSCRIPTION)
                .build();

        var subscriptionsButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button.list"))
                .callbackData(MENU_SUBSCRIPTIONS)
                .build();

        var analyticsButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button.analytics"))
                .callbackData(MENU_ANALYTICS)
                .build();

        var settingsButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button.settings"))
                .callbackData(MENU_SETTINGS)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(addButton))
                .keyboardRow(List.of(subscriptionsButton))
                .keyboardRow(List.of(analyticsButton))
                .keyboardRow(List.of(settingsButton))
                .build();
    }

    public InlineKeyboardMarkup getSubscriptionsKeyboard() {
        var menuButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button"))
                .callbackData(MENU)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(menuButton))
                .build();
    }

    public InlineKeyboardMarkup getConfirmationKeyboard() {
        var editButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit"))
                .callbackData(SUBSCRIPTION_EDIT)
                .build();

        var saveButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.save"))
                .callbackData(SUBSCRIPTION_SAVE)
                .build();

        var restartButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.restart"))
                .callbackData(SUBSCRIPTION_RESTART)
                .build();

        var cancelButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.cancel"))
                .callbackData(SUBSCRIPTION_CANCEL)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(editButton))
                .keyboardRow(List.of(saveButton))
                .keyboardRow(List.of(restartButton))
                .keyboardRow(List.of(cancelButton))
                .build();
    }

    public InlineKeyboardMarkup getEditKeyboard() {
        var editNameButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.name"))
                .callbackData(EDIT_NAME)
                .build();

        var editPriceButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.price"))
                .callbackData(EDIT_PRICE)
                .build();

        var editDateButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.date"))
                .callbackData(EDIT_DATE)
                .build();

        var editCurrencyButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.currency"))
                .callbackData(EDIT_CURRENCY)
                .build();

        var editCategoryButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.category"))
                .callbackData(EDIT_CATEGORY)
                .build();

        var backToConfirmationButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.back"))
                .callbackData(BACK_TO_CONFIRMATION)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(editNameButton, editPriceButton))
                .keyboardRow(List.of(editDateButton, editCurrencyButton))
                .keyboardRow(List.of(editCategoryButton))
                .keyboardRow(List.of(backToConfirmationButton))
                .build();
    }

    public InlineKeyboardMarkup getBackToEditKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(getBackToEditButton()))
                .build();
    }

    private InlineKeyboardButton getBackToEditButton() {
        return InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.back"))
                .callbackData(BACK_TO_EDIT)
                .build();
    }

    public InlineKeyboardMarkup getCurrencySelectionKeyboard() {
        var rubButton = InlineKeyboardButton.builder()
                .text("RUB (₽)")
                .callbackData(CURRENCY_SELECT_PREFIX + "RUB")
                .build();

        var usdButton = InlineKeyboardButton.builder()
                .text("USD ($)")
                .callbackData(CURRENCY_SELECT_PREFIX + "USD")
                .build();

        var eurButton = InlineKeyboardButton.builder()
                .text("EUR (€)")
                .callbackData(CURRENCY_SELECT_PREFIX + "EUR")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(rubButton))
                .keyboardRow(List.of(usdButton))
                .keyboardRow(List.of(eurButton))
                .keyboardRow(List.of(getBackToEditButton()))
                .build();
    }

    public InlineKeyboardMarkup getBackToMenuKeyboard() {
        var backToMenuButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.back"))
                .callbackData(MENU)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(backToMenuButton))
                .build();
    }

    /**
     * Создает календарь для указанного месяца без предварительно выбранной даты.
     * @param yearMonth Месяц и год для отображения.
     * @param backCallbackData callback-дата кнопки назад
     * @return Объект {@link InlineKeyboardMarkup} с календарем.
     */
    public InlineKeyboardMarkup getCalendarKeyboard(YearMonth yearMonth, String backCallbackData) {
        return getCalendarKeyboard(yearMonth, null, backCallbackData);
    }

    /**
     * Основной метод, создающий интерактивную инлайн-клавиатуру с календарем.
     *
     * @param yearMonth     Месяц и год для отображения.
     * @param selectedDate  Дата, которую нужно подсветить (может быть null, если ничего не выбрано).
     * @return Объект {@link InlineKeyboardMarkup} с готовым календарем.
     */
    public InlineKeyboardMarkup getCalendarKeyboard(YearMonth yearMonth, LocalDate selectedDate, String backCallbackData) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createNavigationRow(yearMonth, selectedDate, backCallbackData));
        keyboard.add(createWeekdaysRow());
        keyboard.addAll(createDayGridRows(yearMonth, selectedDate, backCallbackData));
        keyboard.add(createQuickSelectRow(selectedDate, backCallbackData));
        keyboard.add(createActionRow(selectedDate, backCallbackData));

        return new InlineKeyboardMarkup(keyboard);
    }

    /**
     * Создает верхний ряд календаря с кнопками навигации и названием месяца.
     */
    private List<InlineKeyboardButton> createNavigationRow(YearMonth yearMonth, LocalDate selectedDate, String backCallbackData) {
        String selectedDateStr = Optional.ofNullable(selectedDate).map(LocalDate::toString).orElse("");
        String monthName = yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru"));
        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);

        return List.of(
                InlineKeyboardButton.builder().text("←").callbackData(CALENDAR_NAV_PREFIX + prevMonth + "_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text(String.format("%s, %d", monthName, yearMonth.getYear())).callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("→").callbackData(CALENDAR_NAV_PREFIX + nextMonth + "_" + selectedDateStr + "_" + backCallbackData).build()
        );
    }

    /**
     * Создает ряд с названиями дней недели.
     */
    private List<InlineKeyboardButton> createWeekdaysRow() {
        return List.of(
                InlineKeyboardButton.builder().text("Пн").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Вт").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Ср").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Чт").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Пт").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Сб").callbackData(CALENDAR_IGNORE).build(),
                InlineKeyboardButton.builder().text("Вс").callbackData(CALENDAR_IGNORE).build()
        );
    }

    /**
     * Создает сетку с днями месяца.
     */
    private List<List<InlineKeyboardButton>> createDayGridRows(YearMonth yearMonth, LocalDate selectedDate, String backCallbackData) {
        List<List<InlineKeyboardButton>> dayGrid = new ArrayList<>();
        int firstDayOfWeek = yearMonth.atDay(1).getDayOfWeek().getValue();
        int daysInMonth = yearMonth.lengthOfMonth();

        int currentDay = 1;
        for (int i = 0; i < 6 && currentDay <= daysInMonth; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = 1; j <= 7; j++) {
                if ((i == 0 && j < firstDayOfWeek) || currentDay > daysInMonth) {
                    row.add(InlineKeyboardButton.builder().text(" ").callbackData(CALENDAR_IGNORE).build());
                } else {
                    row.add(createDayButton(currentDay, yearMonth, selectedDate, backCallbackData));
                    currentDay++;
                }
            }

            dayGrid.add(row);
        }

        return dayGrid;
    }

    private InlineKeyboardButton createDayButton(int day, YearMonth yearMonth, LocalDate selectedDate, String backCallbackData) {
        LocalDate date = yearMonth.atDay(day);
        String buttonText = String.valueOf(day);
        var buttonBuilder = InlineKeyboardButton.builder();

        if (date.isBefore(LocalDate.now())) {
            buttonBuilder.text(buttonText).callbackData(CALENDAR_NOTIFY_PREFIX + "past_date");
        } else {
            if (Optional.ofNullable(selectedDate).map(d -> d.equals(date)).orElse(false)) {
                buttonText = "✅ " + buttonText;
            } else if (date.equals(LocalDate.now())) {
                buttonText = "🔶 " + buttonText;
            }

            buttonBuilder.text(buttonText).callbackData(CALENDAR_SELECT_PREFIX + date + "_" + backCallbackData);
        }
        return buttonBuilder.build();
    }

    /**
     * Создает ряд с кнопками быстрого выбора периода (+1 месяц, +6 месяцев, +1 год).
     */
    private List<InlineKeyboardButton> createQuickSelectRow(LocalDate selectedDate, String backCallbackData) {
        String selectedDateStr = Optional.ofNullable(selectedDate).map(LocalDate::toString).orElse("");
        return List.of(
                InlineKeyboardButton.builder().text("+1 месяц").callbackData(CALENDAR_QUICK_PREFIX + "1m_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text("+6 месяцев").callbackData(CALENDAR_QUICK_PREFIX + "6m_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text("+1 год").callbackData(CALENDAR_QUICK_PREFIX + "1y_" + selectedDateStr + "_" + backCallbackData).build()
        );
    }

    /**
     * Создает нижний ряд с кнопками действий ("Назад" и "Применить").
     */
    private List<InlineKeyboardButton> createActionRow(LocalDate selectedDate, String backCallbackData) {
        Optional<LocalDate> selectedDateOpt = Optional.ofNullable(selectedDate);
        var applyButton = InlineKeyboardButton.builder().text("Применить");

        if (selectedDateOpt.isPresent()) {
            applyButton.callbackData(CALENDAR_APPLY_PREFIX + selectedDateOpt.get());
        } else {
            applyButton.callbackData(CALENDAR_NOTIFY_PREFIX + "no_date_selected");
        }

        var backButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.back"))
                .callbackData(backCallbackData)
                .build();

        return List.of(backButton, applyButton.build());
    }
}
