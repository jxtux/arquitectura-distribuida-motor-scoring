package com.finanscore.motorscoring.application.security.port.out;
public interface SecretCipherPort {
    String encrypt(String plain);
    String decrypt(String encrypted);
}
