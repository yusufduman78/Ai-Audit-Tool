package com.yusuf.audittool.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yusuf.audittool.model.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage(),
                "Request body must include a payload object."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

