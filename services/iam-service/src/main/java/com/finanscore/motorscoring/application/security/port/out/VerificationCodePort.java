package com.finanscore.motorscoring.application.security.port.out;
public interface VerificationCodePort {
    String generate();
    String hash(String code);
    boolean matches(String code, String hash);
}
