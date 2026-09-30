package com.finanscore.motorscoring.application.security.port.out;
public interface PasswordHasherPort {
    String hash(String raw);
    boolean matches(String raw, String hash);
}
