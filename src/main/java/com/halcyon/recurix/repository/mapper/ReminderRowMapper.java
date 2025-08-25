package com.halcyon.recurix.repository.mapper;

import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@ReadingConverter
public class ReminderRowMapper implements Converter<Row, Reminder> {

    @Override
    public Reminder convert(Row row) {
        return new Reminder(
                row.get("id", Long.class),
                row.get("user_id", Long.class),
                row.get("name", String.class),
                row.get("price", BigDecimal.class),
                row.get("currency", String.class),
                row.get("payment_date", LocalDate.class),
                row.get("category", String.class),
                row.get("renewal_months", Integer.class),
                row.get("telegram_id", Long.class)
        );
    }
}
