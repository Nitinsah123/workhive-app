package com.workhive.common.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) { super(message); }
    public RateLimitException() { super("Too many requests. Please try again later."); }
}
