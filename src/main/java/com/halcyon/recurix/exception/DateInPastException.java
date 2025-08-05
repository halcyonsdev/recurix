package com.halcyon.recurix.exception;

public class DateInPastException extends InvalidDateException {

    public DateInPastException() {
        super("error.date.in_past", "Subscription date cannot be in past");
    }
}
