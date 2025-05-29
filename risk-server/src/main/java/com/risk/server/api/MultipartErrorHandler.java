package com.risk.server.api; // или ваш пакет

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class MultipartErrorHandler {

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipart(MultipartException ex) {
        ex.printStackTrace();  // выведет причину в консоль
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Multipart parse failed: " + ex.getMessage());
    }
}
