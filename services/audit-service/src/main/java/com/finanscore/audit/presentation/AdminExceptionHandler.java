package com.finanscore.audit.presentation;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice(assignableTypes=AdminAuditController.class)
public class AdminExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,Object>> notFound(NoSuchElementException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code","NOT_FOUND","message",ex.getMessage()));
    }
}
