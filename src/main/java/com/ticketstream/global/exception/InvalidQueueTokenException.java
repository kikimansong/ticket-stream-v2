package com.ticketstream.global.exception;

public class InvalidQueueTokenException extends RuntimeException {
    public InvalidQueueTokenException(String message) {
        super(message);
    }
}
