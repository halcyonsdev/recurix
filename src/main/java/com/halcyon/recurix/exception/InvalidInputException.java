package com.halcyon.recurix.exception;

import lombok.Getter;

public class InvalidInputException extends RuntimeException {

    @Getter
    private final String messageCode;

    public InvalidInputException(String messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
    }
}
