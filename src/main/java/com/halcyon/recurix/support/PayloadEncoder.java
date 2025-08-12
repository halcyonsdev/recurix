package com.halcyon.recurix.support;

import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Утилитарный компонент для кодирования и декодирования параметров,
 * передаваемых в callback-запросах или командах.
 */
@Component
public class PayloadEncoder {

    /**
     * Простая запись для хранения декодированных данных.
     * @param subscriptionId ID подписки.
     * @param pageNumber Номер страницы.
     * @param messageId ID сообщения для редактирования или удаления.
     */
    public record Payload(Long subscriptionId, int pageNumber, Integer messageId) {}

    private static final String DELIMITER = ":";

    /**
     * Кодирует ID подписки и номер страницы в одну URL-safe Base64 строку.
     *
     * @param subscriptionId ID подписки.
     * @param pageNumber Номер страницы.
     * @param messageId ID сообщения для редактирования или удаления.
     * @return Закодированная строка.
     */
    public String encode(Long subscriptionId, int pageNumber, Integer messageId) {
        String data = subscriptionId + DELIMITER + pageNumber + DELIMITER + messageId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes());
    }

    /**
     * Декодирует Base64 строку обратно в объект Payload.
     *
     * @param encodedPayload Закодированная строка.
     * @return Объект {@link Payload} с ID и номером страницы.
     * @throws IllegalArgumentException если строка имеет неверный формат.
     */
    public Payload decode(String encodedPayload) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPayload);
            String decodedData = new String(decodedBytes);

            String[] parts = decodedData.split(DELIMITER);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid payload structure after decoding.");
            }

            Long subscriptionId = Long.parseLong(parts[0]);
            int pageNumber = Integer.parseInt(parts[1]);
            Integer messageId = Integer.parseInt(parts[2]);

            return new Payload(subscriptionId, pageNumber, messageId);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode or parse payload: " + encodedPayload, e);
        }
    }
}
