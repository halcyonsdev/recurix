package com.halcyon.recurix.support;

import com.halcyon.recurix.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeriodFormatter {

    private final LocalMessageService messageService;

    public String format(Integer months) {
        if (months == null) {
            return messageService.getMessage("placeholder.not_specified");
        }

        return switch (months) {
            case 1 -> messageService.getMessage("period.monthly");
            case 12 -> messageService.getMessage("period.yearly");
            default -> messageService.getMessage("period.custom", months);
        };
    }
}
