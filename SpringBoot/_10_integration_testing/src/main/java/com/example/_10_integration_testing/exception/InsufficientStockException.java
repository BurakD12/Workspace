package com.example._10_integration_testing.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int onHand, int requested) {
        super("Insufficient stock for " + sku + ": " + onHand +
                " on hand, " + requested + " requested");
    }
}
