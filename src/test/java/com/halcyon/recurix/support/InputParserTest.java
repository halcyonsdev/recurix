package com.halcyon.recurix.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.halcyon.recurix.exception.DateInPastException;
import com.halcyon.recurix.exception.InvalidDateException;
import com.halcyon.recurix.exception.InvalidPriceException;
import com.halcyon.recurix.exception.NegativePriceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Модульные тесты для утилитарного класса {@link InputParser}.
 * <p>
 * Тесты сгруппированы по методам и проверяют как успешные сценарии парсинга,
 * так и корректную обработку различных видов некорректного ввода.
 */
class InputParserTest {

    private final InputParser inputParser = new InputParser();

    @Nested
    @DisplayName("Тестирование парсинга цены (parsePrice)")
    class PriceParsingTests {

        @ParameterizedTest(name = "Вход: \"{0}\", Ожидаемый результат: {1}")
        @CsvSource({
                "'199.99', '199.99'",
                "'25',     '25'",
                "'100',    '100'",
                "'0',      '0'"
        })
        @DisplayName("Должен корректно парсить различные форматы валидной цены")
        void shouldCorrectlyParseValidPrices(String input, String expected) {
            BigDecimal result = inputParser.parsePrice(input);
            assertThat(result).isEqualTo(new BigDecimal(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = { "не число", "10..99", "", " " })
        @DisplayName("Должен выбросить InvalidPriceException для нечислового ввода")
        void shouldThrowInvalidPriceExceptionForNonNumericInput(String invalidInput) {
            assertThrowsExactly(InvalidPriceException.class, () -> inputParser.parsePrice(invalidInput));
        }

        @Test
        @DisplayName("Должен выбросить NegativePriceException для отрицательной цены")
        void shouldThrowNegativePriceExceptionForNegativePrice() {
            assertThrowsExactly(NegativePriceException.class, () -> inputParser.parsePrice("-100"));
        }
    }

    @Nested
    @DisplayName("Тестирование парсинга даты (parseDate)")
    class DateParsingTests {

        @Test
        @DisplayName("Должен корректно парсить валидную дату в будущем")
        void shouldParseValidFutureDate() {
            LocalDate result = inputParser.parseDate("25.12.2099");
            assertThat(result).isEqualTo(LocalDate.of(2099, 12, 25));
        }

        @Test
        @DisplayName("Должен корректно парсить сегодняшнюю дату")
        void shouldParseTodayDate() {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            LocalDate result = inputParser.parseDate(today);
            assertThat(result).isEqualTo(LocalDate.now());
        }

        @ParameterizedTest
        @ValueSource(strings = { "2025-12-25", "32.12.2025", "не дата", "", " " })
        @DisplayName("Должен выбросить InvalidDateException для некорректного формата даты")
        void shouldThrowInvalidDateExceptionForInvalidFormat(String invalidInput) {
            assertThrowsExactly(InvalidDateException.class, () -> inputParser.parseDate(invalidInput));
        }

        @Test
        @DisplayName("Должен выбросить DateInPastException для даты в прошлом")
        void shouldThrowDateInPastExceptionForPastDate() {
            String pastDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            assertThrowsExactly(DateInPastException.class, () -> inputParser.parseDate(pastDate));
        }
    }
}
