package com.halcyon.recurix.service;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        var addButton = getAddButton();

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

    private InlineKeyboardButton getAddButton() {
        return InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button.add"))
                .callbackData(MENU_ADD_SUBSCRIPTION)
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
                InlineKeyboardButton.builder().text(String.format("%s, %d", monthName, yearMonth.getYear())).callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("→").callbackData(CALENDAR_NAV_PREFIX + nextMonth + "_" + selectedDateStr + "_" + backCallbackData).build()
        );
    }

    /**
     * Создает ряд с названиями дней недели.
     */
    private List<InlineKeyboardButton> createWeekdaysRow() {
        return List.of(
                InlineKeyboardButton.builder().text("Пн").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Вт").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Ср").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Чт").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Пт").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Сб").callbackData(IGNORE).build(),
                InlineKeyboardButton.builder().text("Вс").callbackData(IGNORE).build()
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
                    row.add(InlineKeyboardButton.builder().text(" ").callbackData(IGNORE).build());
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

    /**
     * Создает клавиатуру для страницы списка подписок с кнопками пагинации.
     * @param page Объект страницы.
     * @return Инлайн-клавиатура.
     */
    public InlineKeyboardMarkup getSubscriptionsPageKeyboard(Page<Subscription> page, SubscriptionListContext context) {
        var keyboardBuilder = InlineKeyboardMarkup.builder();

        if (page.totalPages() > 1) {
            keyboardBuilder.keyboardRow(createNavigationRow(page));
        }

        keyboardBuilder.keyboardRow(createSortingRow(page.currentPage(), context));
        addKeyboardActionRows(keyboardBuilder);

        return keyboardBuilder.build();
    }

    /**
     * Создает ряд кнопок для пагинации ("назад", "номер страницы", "вперед").
     *
     * @param page Объект страницы для получения информации о текущей странице и общем количестве страниц.
     * @return Список {@link InlineKeyboardButton}, представляющий ряд пагинации.
     */
    private List<InlineKeyboardButton> createNavigationRow(Page<Subscription> page) {
        int currentPage = page.currentPage();

        var backButton = InlineKeyboardButton.builder()
                .text("⬅️")
                .callbackData(currentPage > 0 ? SUB_LIST_PAGE_PREFIX + (currentPage - 1) : IGNORE)
                .build();

        var pageIndicatorButton = InlineKeyboardButton.builder()
                .text(String.format("%d / %d", currentPage + 1, page.totalPages()))
                .callbackData(IGNORE)
                .build();

        var forwardButton = InlineKeyboardButton.builder()
                .text("➡️")
                .callbackData(currentPage < page.totalPages() - 1 ? SUB_LIST_PAGE_PREFIX + (currentPage + 1) : IGNORE)
                .build();

        return List.of(backButton, pageIndicatorButton, forwardButton);
    }

    /**
     * Создает ряд кнопок для сортировки с динамическими индикаторами направления (⬆️/⬇️).
     *
     * @param currentPage Номер текущей страницы для формирования callback-данных.
     * @param context     Текущий контекст сортировки, определяющий, какой индикатор отображать.
     * @return Список {@link InlineKeyboardButton}, представляющий ряд сортировки.
     */
    private List<InlineKeyboardButton> createSortingRow(int currentPage, SubscriptionListContext context) {
        String dateSortText = messageService.getMessage("button.sort_by_date") +
                ("paymentDate".equals(context.sortField()) && context.sortDirection() == Sort.Direction.ASC ? " ⬇️" : " ⬆️");

        String priceSortText = messageService.getMessage("button.sort_by_price") +
                ("price".equals(context.sortField()) && context.sortDirection() == Sort.Direction.ASC ? " ⬇️" : " ⬆️");

        var sortByDateButton = InlineKeyboardButton.builder()
                .text(dateSortText)
                .callbackData(SUB_SORT_PREFIX + "paymentDate_" + currentPage)
                .build();

        var sortByPriceButton = InlineKeyboardButton.builder()
                .text(priceSortText)
                .callbackData(SUB_SORT_PREFIX + "price_" + currentPage)
                .build();

        return List.of(sortByDateButton, sortByPriceButton);
    }

    /**
     * Добавляет ряды с кнопками действий ("Добавить подписку", "Назад в меню") в сборщик клавиатуры.
     *
     * @param keyboardBuilder Сборщик {@link InlineKeyboardMarkup.InlineKeyboardMarkupBuilder}, в который добавляются ряды.
     */
    private void addKeyboardActionRows(InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder) {
        keyboardBuilder.keyboardRow(List.of(getAddButton()));
        keyboardBuilder.keyboardRow(List.of(getMenuButton()));
    }

    private InlineKeyboardButton getMenuButton() {
        return InlineKeyboardButton.builder()
                .text(messageService.getMessage("menu.button"))
                .callbackData(MENU)
                .build();
    }

    /**
     * Создает клавиатуру для экрана детального просмотра подписки.
     * @param subscriptionId ID просматриваемой подписки.
     * @param pageNumber     Номер страницы, на которую нужно вернуться.
     * @return Инлайн-клавиатура с кнопками действий.
     */
    public InlineKeyboardMarkup getSubscriptionDetailKeyboard(Long subscriptionId, int pageNumber) {
        var backToListButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscriptions.list.back"))
                .callbackData(SUB_LIST_PAGE_PREFIX + pageNumber)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(backToListButton))
                .build();
    }
}
