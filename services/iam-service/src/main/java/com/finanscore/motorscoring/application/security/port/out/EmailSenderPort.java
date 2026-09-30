package com.finanscore.motorscoring.application.security.port.out;
public interface EmailSenderPort {
    void sendEmailVerificationCode(String email, String displayName, String code);
}
