package com.halcyon.recurix.service;

import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalMessageService {

    private final MessageSource messageSource;
    private final Locale locale = Locale.of("ru");

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, locale);
    }

    public String getMessage(String code, Object... args) {
        String notSpecifiedPlaceholder = messageSource.getMessage("placeholder.not_specified", null, locale);

        Object[] processedArgs = Arrays.stream(args)
                .map(arg -> (arg == null
                        ? notSpecifiedPlaceholder
                        : arg))
                .toArray();

        return messageSource.getMessage(code, processedArgs, locale);
    }
}
