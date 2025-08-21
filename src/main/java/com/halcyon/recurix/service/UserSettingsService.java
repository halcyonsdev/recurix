package com.halcyon.recurix.service;

import com.halcyon.recurix.model.RecurixUser;
import com.halcyon.recurix.model.UserSettings;
import com.halcyon.recurix.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Сервис для управления настройками пользователя.
 */
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;

    /**
     * Получает настройки для указанного пользователя.
     * Если настроек нет, создает их со значениями по умолчанию.
     *
     * @param user Внутренний пользователь системы.
     * @return Mono с актуальными настройками.
     */
    public Mono<UserSettings> getSettings(RecurixUser user) {
        return settingsRepository.findByUserId(user.id())
                .switchIfEmpty(
                        Mono.defer(() -> {
                            UserSettings defaultSettings = UserSettings.createDefault(user.id());
                            return settingsRepository.save(defaultSettings);
                        }));
    }

    /**
     * Сохраняет (обновляет) настройки пользователя.
     * 
     * @param settings Объект с настройками для сохранения.
     * @return Mono с сохраненными настройками.
     */
    public Mono<UserSettings> save(UserSettings settings) {
        return settingsRepository.save(settings);
    }
}
