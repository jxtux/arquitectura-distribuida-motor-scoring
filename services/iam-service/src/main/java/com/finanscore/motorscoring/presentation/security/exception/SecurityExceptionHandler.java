package com.finanscore.motorscoring.presentation.security.exception;

import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** IAM errors must be resolved before the catch-all GlobalExceptionHandler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(SecurityApplicationException.class)
    ResponseEntity<?> handle(SecurityApplicationException e) {
        HttpStatus status = switch (e.code()) {
            case "EMAIL_ALREADY_EXISTS", "EXPLICIT_LINK_REQUIRED", "MFA_ALREADY_ENABLED" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS", "INVALID_MFA_SETUP_TOKEN", "INVALID_MFA_CHALLENGE_TOKEN" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_NOT_AVAILABLE" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.warn("IAM request rejected: code={}, status={}", e.code(), status.value());
        return ResponseEntity.status(status).body(Map.of("code", e.code(), "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String,String> violations = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(x -> violations.putIfAbsent(x.getField(), x.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
            "code", "INVALID_REQUEST",
            "message", "La solicitud contiene campos inválidos.",
            "violations", violations));
    }
}
