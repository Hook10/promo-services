package com.kas.promoservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, String>> handleException(WebExchangeBindException e) {
        final BindingResult bindingResult = e.getBindingResult();
        final List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        final Map<String, String> errors = new HashMap<>();
        fieldErrors.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PromoNotFoundException.class)
    public Mono<ResponseEntity<String>> handleBookNotFoundException(PromoNotFoundException ex) {
        log.error("BookNotFoundException occurred: {}", ex.getMessage(), ex);
        return Mono.just(new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Map<String, String>> handleEnumError(ServerWebInputException ex) {
        Map<String, String> errors = new HashMap<>();

        Throwable cause = ex.getMostSpecificCause();
        log.error("Enum status is missing or incorrect: {}", cause.getMessage());
        if (cause.getMessage().contains("No enum constant")) {
            try {
                String message = cause.getMessage();
                String enumClassName = message.substring(
                        "No enum constant ".length(),
                        message.lastIndexOf('.')
                );

                Class<?> enumClass = Class.forName(enumClassName);
                if (enumClass.isEnum()) {
                    Object[] constants = enumClass.getEnumConstants();
                    errors.put("error", "Invalid value for " + enumClass.getSimpleName()
                            + ". Allowed values: " + Arrays.toString(constants));
                    return ResponseEntity.badRequest().body(errors);
                }
            } catch (Exception exception) {
                log.error("Enum class not found while parsing error: {}", exception.getMessage());
            }
        }

        errors.put("error", "Invalid request");
        errors.put("message", cause.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleException(Exception ex) {
        log.error("Exception occurred: {}", ex.getMessage(), ex);
        return Mono.just(new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
}

