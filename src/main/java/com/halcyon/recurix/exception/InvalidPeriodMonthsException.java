package com.halcyon.recurix.exception;

public class InvalidPeriodMonthsException extends InvalidInputException {

    public InvalidPeriodMonthsException() {
        super("error.format.months", "Invalid period months format");
    }

    public InvalidPeriodMonthsException(String messageCode, String message) {
        super(messageCode, message);
    }
}
