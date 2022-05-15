package com.example.diploma.controller.error;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@NoArgsConstructor
public class ApiError {

    @NotNull
    private ZonedDateTime timestamp;
    @NotNull
    private Integer status;
    @NotNull
    private String message;

    public ApiError(HttpStatus httpStatus, String message) {
        this.timestamp = ZonedDateTime.now();
        this.status = httpStatus.value();
        this.message = message;
    }

    public static ApiError createError(HttpStatus httpStatus, String message) {
        return new ApiError(httpStatus, message);
    }
}
