package com.halcyon.recurix.exception;

public class InvalidDateException extends InvalidInputException {

    public InvalidDateException() {
        super("error.format.date", "Invalid date format");
    }

    public InvalidDateException(String messageCode, String message) {
        super(messageCode, message);
    }
}
