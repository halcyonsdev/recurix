package com.halcyon.recurix.service;

import com.halcyon.recurix.RecurixBot;
import com.halcyon.recurix.dto.ReminderDto;
import com.halcyon.recurix.repository.SubscriptionRepository;
import com.halcyon.recurix.support.PayloadEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Сервис для фоновой рассылки напоминаний о подписках.
 * <p>
 * Ежедневно находит подписки с подходящей датой платежа
 * и отправляет уведомления пользователям
 */
@Service
@Slf4j
public class ReminderService {

    private final SubscriptionRepository subscriptionRepository;
    private final LocalMessageService messageService;
    private final RecurixBot recurixBot;
    private final PayloadEncoder payloadEncoder;

    public ReminderService(
            SubscriptionRepository subscriptionRepository,
            LocalMessageService messageService,
            @Lazy RecurixBot recurixBot,
            PayloadEncoder payloadEncoder
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.messageService = messageService;
        this.recurixBot = recurixBot;
        this.payloadEncoder = payloadEncoder;
    }

    /**
     * Запускается каждый день в 9:00 по московскому времени для отправки напоминаний.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Moscow")
    public void sendDailyReminders() {
        log.info("SCHEDULER: Starting daily reminder task...");

        subscriptionRepository.findAllForReminding()
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::sendReminderMessage)
                .sequential()
                .count()
                .doOnSuccess(count -> log.info("SCHEDULER: Daily reminder task finished. Processed {} reminders.", count))
                .doOnError(e -> log.error("SCHEDULER: A critical error occurred during the reminder task.", e))
                .subscribe();
    }

    /**
     * Отправляет сообщение с напоминанием пользователю.
     *
     * @param reminderDto объект с данными для напоминания.
     * @return {@code Mono<Void>}, завершающийся после отправки.
     */
    private Mono<Void> sendReminderMessage(ReminderDto reminderDto) {
        String payload = payloadEncoder.encode(reminderDto.id(), 0, 0);
        String viewCommand = "/view_" + payload;

        String messageText = messageService.getMessage(
                "reminder.message",
                reminderDto.name(),
                reminderDto.price(),
                viewCommand
        );

        var message = SendMessage.builder()
                .chatId(reminderDto.telegramId())
                .text(messageText)
                .parseMode(ParseMode.HTML)
                .build();

        return Mono.fromRunnable(() -> {
            try {
                recurixBot.execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException("Failed to send telegram message", e);
            }
        });
    }
}
