package com.halcyon.recurix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static com.halcyon.recurix.callback.CallbackData.*;

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
}
