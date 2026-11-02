package com.example._9_docker.exception;

public class InsufficientStockException extends RuntimeException{
    public InsufficientStockException(String sku, int onHand, int requested) {
        super("Insufficient stock for " + sku + ": "+ onHand +
         " on hand, " + requested + " requested");
    }
}
