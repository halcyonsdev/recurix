package com.halcyon.recurix.command;

import java.util.Collections;
import java.util.List;

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

import com.halcyon.recurix.TestDataBuilder;
import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.service.KeyboardService;
import com.halcyon.recurix.service.SubscriptionService;
import com.halcyon.recurix.service.UserService;
import com.halcyon.recurix.support.SubscriptionMessageFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Модульные тесты для класса {@link ListCommand}.
 */
@ExtendWith(MockitoExtension.class)
class ListCommandTest {
    
    @Mock
    private UserService userService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private KeyboardService keyboardService;

    @Mock
    private SubscriptionMessageFactory subscriptionMessageFactory;

    @InjectMocks
    private ListCommand command;

    private RecurixUser mockUser;
    private InlineKeyboardMarkup expectedKeyboard;

    @BeforeEach
    void setUp() {
        this.mockUser = new RecurixUser(
                1L,
                TestDataBuilder.DEFAULT_USER_ID,
                TestDataBuilder.DEFAULT_FIRST_NAME,
                null
        );
        this.expectedKeyboard = new InlineKeyboardMarkup();

        when(userService.findOrCreateUser(any(User.class))).thenReturn(Mono.just(mockUser));
        when(keyboardService.getSubscriptionsKeyboard()).thenReturn(expectedKeyboard);
    }

    @Test
    @DisplayName("Должен отобразить список подписок, если они есть у пользователья")
    void execute_shouldDisplaySubscriptions_whenUserHasThem() {
        Update update = TestDataBuilder.buildUpdateWithMessage("/list");
        String formattedListText = "Formatted list of subscriptions";
        List<Subscription> subscriptions = List.of(new Subscription(), new Subscription());

        when(subscriptionService.getAllByUserId(mockUser.id())).thenReturn(Flux.fromIterable(subscriptions));
        when(subscriptionMessageFactory.formatSubscriptionList(subscriptions)).thenReturn(formattedListText);

        Mono<?> resultMono = command.execute(update);

        StepVerifier.create(resultMono)
                .assertNext(botApiMethod -> {
                    assertThat(botApiMethod).isInstanceOf(SendMessage.class);
                    SendMessage sendMessage = (SendMessage) botApiMethod;

                    assertThat(sendMessage.getChatId()).isEqualTo(TestDataBuilder.DEFAULT_CHAT_ID.toString());
                    assertThat(sendMessage.getText()).isEqualTo(formattedListText);
                    assertThat(sendMessage.getReplyMarkup()).isEqualTo(expectedKeyboard);
            
                })
                .verifyComplete();

        verify(subscriptionMessageFactory).formatSubscriptionList(subscriptions);
    }

    @Test
    @DisplayName("Должен отобразить сообщение об отсутствии подписок, если их нет")
    void execute_shouldDisplayEmptyMessage_whenUserHasNoSubscriptions() {
        Update update = TestDataBuilder.buildUpdateWithMessage("/list");
        String noSubscriptionsText = "You have no subscriptions";
        List<Subscription> emptyList = Collections.emptyList();

        when(subscriptionService.getAllByUserId(mockUser.id())).thenReturn(Flux.empty());
        when(subscriptionMessageFactory.formatSubscriptionList(emptyList)).thenReturn(noSubscriptionsText);

        Mono<?> resultMono = command.execute(update);

        StepVerifier.create(resultMono)
                .assertNext(botApiMethod -> {
                    assertThat(botApiMethod).isInstanceOf(SendMessage.class);
                    SendMessage sendMessage = (SendMessage) botApiMethod;

                    assertThat(sendMessage.getChatId()).isEqualTo(TestDataBuilder.DEFAULT_CHAT_ID.toString());
                    assertThat(sendMessage.getText()).isEqualTo(noSubscriptionsText);
                    assertThat(sendMessage.getReplyMarkup()).isEqualTo(expectedKeyboard);
                })
                .verifyComplete();

        verify(subscriptionMessageFactory).formatSubscriptionList(emptyList);
    }
}
