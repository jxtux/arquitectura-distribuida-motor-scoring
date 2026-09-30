package com.finanscore.motorscoring.infrastructure.security.crypto;

import com.finanscore.motorscoring.application.security.port.out.PasswordHasherPort;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public final class Argon2PasswordHasherAdapter implements PasswordHasherPort {
    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    @Override public String hash(String raw) { return encoder.encode(raw); }
    @Override public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
}
