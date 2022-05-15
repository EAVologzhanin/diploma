package com.example.diploma.model.exception;

import org.springframework.http.HttpStatus;

public class IllegalInputParameterException extends BaseException{

    public IllegalInputParameterException(String message) {
        super(message);
    }

    @Override
    public String getUserMessage() {
        return getMessage();
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
