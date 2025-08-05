package com.halcyon.recurix.exception;

public class NegativePriceException extends InvalidPriceException {

    public NegativePriceException() {
        super("error.format.price", "Invalid price format");
    }
}
