package com.halcyon.recurix.exception;

public class InvalidPriceException extends InvalidInputException {

    public InvalidPriceException() {
        super("error.format.price", "Invalid price format");
    }

    public InvalidPriceException(String messageCode, String message) {
        super(messageCode, message);
    }
}
