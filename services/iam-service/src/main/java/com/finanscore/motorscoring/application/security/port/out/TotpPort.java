package com.finanscore.motorscoring.application.security.port.out;
public interface TotpPort {
    String generateSecret();
    String buildOtpAuthUri(String issuer, String accountLabel, String secret);
    boolean verify(String secret, String code);
}
