package com.halcyon.recurix.support;

import com.halcyon.recurix.exception.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

/**
 * Утилитарный компонент для парсинга и валидации пользовательского ввода.
 * <p>
 * Инкапсулирует логику преобразования строк в доменные типы и выбрасывает
 * типизированные, кастомные исключения в случае некорректных данных.
 */
@Component
public class InputParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Парсит и валидирует строку для получения цены.
     * 
     * @param text Входной текст от пользователя.
     * @return цена BigDecimal.
     * @throws InvalidPriceException  если текст имеет неверный числовой формат.
     * @throws NegativePriceException если цена является отрицательным числом.
     */
    public BigDecimal parsePrice(String text) throws InvalidPriceException, NegativePriceException {
        if (text == null) {
            throw new InvalidPriceException();
        }

        try {
            BigDecimal price = new BigDecimal(text.replace(',', '.'));
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NegativePriceException();
            }
            return price;
        } catch (NumberFormatException e) {
            throw new InvalidPriceException();
        }
    }

    /**
     * Парсит и валидирует строку для получения даты.
     * 
     * @param text Входной текст от пользователя.
     * @return дата LocalDate.
     * @throws InvalidDateException если текст имеет неверный формат даты.
     * @throws DateInPastException  если дата находится в прошлом.
     */
    public LocalDate parseDate(String text) throws InvalidDateException, DateInPastException {
        if (text == null) {
            throw new InvalidDateException();
        }
        try {
            LocalDate date = LocalDate.parse(text, DATE_FORMATTER);
            if (date.isBefore(LocalDate.now())) {
                throw new DateInPastException();
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new InvalidDateException();
        }
    }

    /**
     * Парсит и валидирует строку для получения периода.
     * 
     * @param text Входной текст от пользователя.
     * @return количество месяцев.
     * @throws InvalidPeriodMonthsException если текст имеет неверный числовой формат.
     */
    public Integer parsePeriodMonths(String text) throws InvalidPeriodMonthsException {
        if (text == null) {
            throw new InvalidPeriodMonthsException();
        }

        try {
            int months = Integer.parseInt(text);
            if (months < 0) {
                throw new InvalidPeriodMonthsException();
            }

            return months;
        } catch (NumberFormatException e) {
            throw new InvalidPeriodMonthsException();
        }
    }
}
