package com.halcyon.recurix.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Хранит персональные, изменяемые настройки пользователя.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column("id")
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("reminders_enabled")
    private boolean remindersEnabled;

    @Column("reminder_days_before")
    private int reminderDaysBefore;

    /**
     * Создает объект настроек по умолчанию для нового пользователя.
     *
     * @param userId ID пользователя из таблицы users.
     * @return Настройки по умолчанию (напоминания включены, за 3 дня).
     */
    public static UserSettings createDefault(Long userId) {
        return UserSettings.builder()
                .userId(userId)
                .remindersEnabled(true)
                .reminderDaysBefore(3)
                .build();
    }
}
