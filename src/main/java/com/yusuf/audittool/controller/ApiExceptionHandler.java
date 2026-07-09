package com.yusuf.audittool.controller;

import java.io.UncheckedIOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yusuf.audittool.agent.AgentRuntimeException;
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

    @ExceptionHandler(AgentRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleAgentRuntime(AgentRuntimeException exception) {
        String details = "Agent returned an empty response.".equals(exception.getMessage())
                ? "The local model did not produce a usable output."
                : "Could not connect to Ollama.";

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(exception.getMessage(), details));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ErrorResponse> handlePromptTemplate(UncheckedIOException exception) {
        ErrorResponse response = new ErrorResponse(
                "Prompt template is not available.",
                "Could not read the configured prompt template."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
