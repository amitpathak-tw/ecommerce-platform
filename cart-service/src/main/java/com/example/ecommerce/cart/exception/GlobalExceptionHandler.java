package com.example.ecommerce.cart.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CartNotFoundException.class)
    ResponseEntity<ErrorResponse> cartNotFound(CartNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    ResponseEntity<ErrorResponse> itemNotFound(CartItemNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(CartStateException.class)
    ResponseEntity<ErrorResponse> invalidState(CartStateException exception, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_REQUEST", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> requestBodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage(),
                        (left, right) -> left
                ));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> requestParameterValidation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage() == null ? "Invalid value" : violation.getMessage(),
                        (left, right) -> left
                ));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> fallback(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> errors
    ) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), status.value(), code, message, request.getRequestURI(), errors));
    }
}
