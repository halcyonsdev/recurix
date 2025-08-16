package com.halcyon.recurix.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.halcyon.recurix.TestDataBuilder;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.service.ConversationStateService;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.LocalMessageService;
import com.halcyon.recurix.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Модульные тесты для класса {@link StartCommand}.
 */
@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    @Mock
    private UserService userService;

    @Mock
    private LocalMessageService messageService;

    @Mock
    private KeyboardService keyboardService;

    @Mock
    private ConversationStateService stateService;

    @InjectMocks
    private StartCommand command;

    private InlineKeyboardMarkup expectedKeyboard;

    @BeforeEach
    void setUp() {
        RecurixUser mockUserInDb = new RecurixUser(
                1L,
                TestDataBuilder.DEFAULT_USER_ID,
                TestDataBuilder.DEFAULT_FIRST_NAME,
                null);
        this.expectedKeyboard = new InlineKeyboardMarkup();

        when(stateService.clearState(TestDataBuilder.DEFAULT_USER_ID)).thenReturn(Mono.empty());
        when(userService.findOrCreateUser(any(User.class))).thenReturn(Mono.just(mockUserInDb));
        when(keyboardService.getMainMenuKeyboard()).thenReturn(expectedKeyboard);
    }

    @Test
    @DisplayName("Команда /start должна сбрасывать состояние и отправлять приветственное сообщение")
    void execute_shouldClearStateAndSendWelcomeMessage() {
        Update update = TestDataBuilder.buildUpdateWithMessage("/start");
        String expectedText = "Welcome, Tester!";

        when(messageService.getMessage("welcome.message")).thenReturn(expectedText);

        Mono<?> resultMono = command.execute(update);

        StepVerifier.create(resultMono)
                .assertNext(botApiMethod -> {
                    assertThat(botApiMethod).isInstanceOf(SendMessage.class);
                    SendMessage sendMessage = (SendMessage) botApiMethod;

                    assertThat(sendMessage.getChatId()).isEqualTo(TestDataBuilder.DEFAULT_CHAT_ID.toString());
                    assertThat(sendMessage.getText()).isEqualTo(expectedText);
                    assertThat(sendMessage.getReplyMarkup()).isEqualTo(expectedKeyboard);
                })
                .verifyComplete();

        verify(stateService).clearState(TestDataBuilder.DEFAULT_USER_ID);
        verify(userService).findOrCreateUser(update.getMessage().getFrom());
    }
}
