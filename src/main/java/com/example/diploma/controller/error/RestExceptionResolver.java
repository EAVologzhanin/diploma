package com.example.diploma.controller.error;

import com.example.diploma.model.exception.BaseException;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class RestExceptionResolver extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<ApiError> handleBaseException(BaseException exception, HttpServletRequest httpServletRequest) {
        return innerHandleException(exception, exception.getStatusCode(), exception.getUserMessage(), httpServletRequest);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ResponseEntity<ApiError> handleThrowable(BaseException exception, HttpServletRequest httpServletRequest) {
        Sentry.captureException(exception);
        return innerHandleException(exception, HttpStatus.INTERNAL_SERVER_ERROR, "unknown error", httpServletRequest);
    }

    protected ResponseEntity<ApiError> innerHandleException(Throwable ex, HttpStatus httpStatus, String message, HttpServletRequest httpServletRequest) {
        Map<String, String> headers = new HashMap<>();
        httpServletRequest
                .getHeaderNames()
                .asIterator()
                .forEachRemaining(headerName -> headers.put(headerName, headers.get(headerName)));
        return innerHandleException(ex, httpStatus, message, httpServletRequest.getMethod(), httpServletRequest.getRequestURI(), headers);
    }

    protected ResponseEntity<ApiError> innerHandleException(Throwable ex, HttpStatus httpStatus, String message, String method, String uri, Map<String, ?> headers) {
       final ApiError error = ApiError.createError(httpStatus, message);
       log.error(String.valueOf(ex), httpStatus, method, uri, headers);
       return new ResponseEntity<>(error, httpStatus);
    }
}
