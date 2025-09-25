package com.halcyon.recurix.service;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.model.UserSettings;
import com.halcyon.recurix.service.context.SubscriptionListContext;
import com.halcyon.recurix.service.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    public InlineKeyboardMarkup getEditKeyboard(String backCallbackData) {
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

        var editCategoryButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.category"))
                .callbackData(EDIT_CATEGORY)
                .build();

        var editPeriodButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.edit.period"))
                .callbackData(EDIT_PERIOD)
                .build();

        var backToConfirmationButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.back"))
                .callbackData(backCallbackData)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(editNameButton, editPriceButton))
                .keyboardRow(List.of(editDateButton, editCategoryButton))
                .keyboardRow(List.of(editPeriodButton))
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
     * 
     * @param yearMonth        Месяц и год для отображения.
     * @param backCallbackData callback-дата кнопки назад
     * @return Объект {@link InlineKeyboardMarkup} с календарем.
     */
    public InlineKeyboardMarkup getCalendarKeyboard(YearMonth yearMonth, String backCallbackData) {
        return getCalendarKeyboard(yearMonth, null, backCallbackData);
    }

    /**
     * Основной метод, создающий интерактивную инлайн-клавиатуру с календарем.
     *
     * @param yearMonth    Месяц и год для отображения.
     * @param selectedDate Дата, которую нужно подсветить (может быть null, если ничего не выбрано).
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
                InlineKeyboardButton.builder().text("←")
                        .callbackData(CALENDAR_NAV_PREFIX + prevMonth + "_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text(String.format("%s, %d", monthName, yearMonth.getYear())).callbackData(IGNORE)
                        .build(),
                InlineKeyboardButton.builder().text("→")
                        .callbackData(CALENDAR_NAV_PREFIX + nextMonth + "_" + selectedDateStr + "_" + backCallbackData).build());
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
                InlineKeyboardButton.builder().text("Вс").callbackData(IGNORE).build());
    }

    /**
     * Создает сетку с днями месяца.
     */
    private List<List<InlineKeyboardButton>>
            createDayGridRows(YearMonth yearMonth, LocalDate selectedDate, String backCallbackData) {
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
                InlineKeyboardButton.builder().text("+1 месяц")
                        .callbackData(CALENDAR_QUICK_PREFIX + "1m_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text("+6 месяцев")
                        .callbackData(CALENDAR_QUICK_PREFIX + "6m_" + selectedDateStr + "_" + backCallbackData).build(),
                InlineKeyboardButton.builder().text("+1 год")
                        .callbackData(CALENDAR_QUICK_PREFIX + "1y_" + selectedDateStr + "_" + backCallbackData).build());
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
     * Создает клавиатуру для выбора периода списания (ежемесячно, ежегодно, другой).
     *
     * @return Объект {@link InlineKeyboardMarkup} с готовой клавиатурой.
     */
    public InlineKeyboardMarkup getPeriodSelectionKeyboard() {
        var monthlyButton = InlineKeyboardButton.builder()
                .text("Ежемесячно")
                .callbackData(PERIOD_SELECT_PREFIX + "1")
                .build();

        var yearlyButton = InlineKeyboardButton.builder()
                .text("Ежегодно")
                .callbackData(PERIOD_SELECT_PREFIX + "12")
                .build();

        var customButton = InlineKeyboardButton.builder()
                .text("Другой период...")
                .callbackData(PERIOD_SELECT_CUSTOM)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(monthlyButton, yearlyButton))
                .keyboardRow(List.of(customButton))
                .keyboardRow(List.of(getBackToEditButton()))
                .build();
    }

    /**
     * Создает клавиатуру для страницы списка подписок с кнопками пагинации.
     * 
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
     * @param page Объект страницы для получения информации о текущей странице и общем количестве
     *             страниц.
     * @return Список {@link InlineKeyboardButton}, представляющий ряд пагинации.
     */
    private List<InlineKeyboardButton> createNavigationRow(Page<Subscription> page) {
        int currentPage = page.currentPage();

        var backButton = InlineKeyboardButton.builder()
                .text("⬅️")
                .callbackData(currentPage > 0
                        ? SUB_LIST_PAGE_PREFIX + (currentPage - 1)
                        : IGNORE)
                .build();

        var pageIndicatorButton = InlineKeyboardButton.builder()
                .text(String.format("%d / %d", currentPage + 1, page.totalPages()))
                .callbackData(IGNORE)
                .build();

        var forwardButton = InlineKeyboardButton.builder()
                .text("➡️")
                .callbackData(currentPage < page.totalPages() - 1
                        ? SUB_LIST_PAGE_PREFIX + (currentPage + 1)
                        : IGNORE)
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
        String dateSortText = messageService.getMessage("subscription.button.sort_by_date") +
                ("paymentDate".equals(context.sortField()) && context.sortDirection() == Sort.Direction.ASC
                        ? " ⬇️"
                        : " ⬆️");

        String priceSortText = messageService.getMessage("subscription.button.sort_by_price") +
                ("price".equals(context.sortField()) && context.sortDirection() == Sort.Direction.ASC
                        ? " ⬇️"
                        : " ⬆️");

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
     * @param keyboardBuilder Сборщик {@link InlineKeyboardMarkup.InlineKeyboardMarkupBuilder}, в
     *                        который добавляются ряды.
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
     * 
     * @param subscriptionId ID просматриваемой подписки.
     * @param pageNumber     Номер страницы, на которую нужно вернуться.
     * @return Инлайн-клавиатура с кнопками действий.
     */
    public InlineKeyboardMarkup getSubscriptionDetailKeyboard(Long subscriptionId, int pageNumber) {
        var editButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscription.button.edit"))
                .callbackData(SUB_EDIT_DETAIL_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        var deleteButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscription.button.delete"))
                .callbackData(SUB_DELETE_CONFIRM_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        var backToListButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscriptions.list.back"))
                .callbackData(SUB_LIST_PAGE_PREFIX + pageNumber)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(editButton, deleteButton))
                .keyboardRow(List.of(backToListButton))
                .build();
    }

    /**
     * Создает клавиатуру для подтверждения изменений после редактирования.
     *
     * @param subscriptionId ID подписки.
     * @param pageNumber     Номер страницы для возврата.
     * @return Инлайн-клавиатура.
     */
    public InlineKeyboardMarkup getEditConfirmationKeyboard(Long subscriptionId, int pageNumber) {
        var updateButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscription.button.save"))
                .callbackData(SUB_UPDATE_AND_VIEW_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        var cancelButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("subscription.button.cancel"))
                .callbackData(SUB_CANCEL_EDIT_AND_VIEW_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(updateButton))
                .keyboardRow(List.of(cancelButton))
                .build();
    }

    /**
     * Создает клавиатуру для подтверждения удаления подписки.
     *
     * @param subscriptionId ID подписки.
     * @param pageNumber     Номер страницы для возврата.
     * @return Клавиатура с кнопками "Да" и "Нет".
     */
    public InlineKeyboardMarkup getDeleteConfirmationKeyboard(Long subscriptionId, int pageNumber) {
        var confirmButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.delete.confirm"))
                .callbackData(SUB_DELETE_EXECUTE_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        var cancelButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("dialog.button.delete.cancel"))
                .callbackData(SUB_VIEW_PREFIX + subscriptionId + "_" + pageNumber)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(confirmButton, cancelButton))
                .build();
    }

    /**
     * Создает клавиатуру для меню настроек.
     * Динамически отображает текущее состояние настроек.
     *
     * @param settings Текущие настройки пользователя.
     * @return Клавиатура меню настроек.
     */
    public InlineKeyboardMarkup getSettingsKeyboard(UserSettings settings) {
        String statusKey = settings.isRemindersEnabled()
                ? "settings.status.enabled"
                : "settings.status.disabled";
        String statusText = messageService.getMessage(statusKey);

        var toggleButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("settings.button.reminders", statusText))
                .callbackData(SETTINGS_TOGGLE_REMINDERS)
                .build();

        var day1 = createDayButton(1, settings.getReminderDaysBefore());
        var day3 = createDayButton(3, settings.getReminderDaysBefore());
        var day7 = createDayButton(7, settings.getReminderDaysBefore());

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(toggleButton))
                .keyboardRow(List.of(day1, day3, day7))
                .keyboardRow(List.of(getMenuButton()))
                .build();
    }

    /**
     * Создает инлайн-кнопку для выбора количества дней.
     *
     * @param days        Количество дней, которое представляет эта кнопка (например, 1, 3, 7).
     * @param currentDays Текущее выбранное пользователем количество дней.
     * @return Объект {@link InlineKeyboardButton}, готовый для добавления в клавиатуру.
     */
    private InlineKeyboardButton createDayButton(int days, int currentDays) {
        String selectedChar = (days == currentDays)
                ? "✅"
                : " ";
        String text = String.format(
                "%s%s %s %s",
                selectedChar,
                messageService.getMessage("settings.button.days_prefix"),
                days,
                messageService.getMessage("settings.button.days_suffix"));

        return InlineKeyboardButton.builder()
                .text(text.trim())
                .callbackData(SETTINGS_CHANGE_DAYS_PREFIX + days)
                .build();
    }

    /**
     * Создает клавиатуру для меню аналитики.
     *
     * @param yearMonth Текущий отображаемый месяц.
     * @return Клавиатура с кнопками навигации.
     */
    public InlineKeyboardMarkup getAnalyticsKeyboard(YearMonth yearMonth) {
        YearMonth prevMonth = yearMonth.minusMonths(1);
        YearMonth nextMonth = yearMonth.plusMonths(1);

        var prevButton = InlineKeyboardButton.builder()
                .text("⬅️ " + prevMonth.format(DateTimeFormatter.ofPattern("MMM")))
                .callbackData(ANALYTICS_NAV_PREFIX + prevMonth)
                .build();

        var thisMonthButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("analytics.button.this_month"))
                .callbackData(IGNORE)
                .build();

        var nextButton = InlineKeyboardButton.builder()
                .text(nextMonth.format(DateTimeFormatter.ofPattern("MMM")) + " ➡️")
                .callbackData(ANALYTICS_NAV_PREFIX + nextMonth)
                .build();

        // TODO: Добавить логику для кнопки "За весь год"
        var byYearButton = InlineKeyboardButton.builder()
                .text(messageService.getMessage("analytics.button.by_year"))
                .callbackData(ANALYTICS_BY_YEAR)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(prevButton, thisMonthButton, nextButton))
                .keyboardRow(List.of(byYearButton))
                .keyboardRow(List.of(getMenuButton()))
                .build();
    }
}
