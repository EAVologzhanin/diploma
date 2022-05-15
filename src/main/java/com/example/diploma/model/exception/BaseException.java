package com.example.diploma.model.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseException extends RuntimeException {
    protected BaseException(String message) {
        super(message);
    }

    public abstract String getUserMessage();

    public abstract HttpStatus getStatusCode();
}
