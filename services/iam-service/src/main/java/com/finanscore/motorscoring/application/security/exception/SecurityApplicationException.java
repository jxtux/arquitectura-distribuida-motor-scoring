package com.finanscore.motorscoring.application.security.exception;
public class SecurityApplicationException extends RuntimeException {
    private final String code;
    public SecurityApplicationException(String code, String message) {
        super(message); this.code = code;
    }
    public String code() { return code; }
}
