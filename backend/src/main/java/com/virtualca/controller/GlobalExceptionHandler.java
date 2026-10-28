package com.virtualca.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "error");
        StringBuilder sb = new StringBuilder();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage());
        }
        resp.put("message", sb.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleClientErrors(RuntimeException ex) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "error");
        resp.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralErrors(Exception ex) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "error");
        resp.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}

