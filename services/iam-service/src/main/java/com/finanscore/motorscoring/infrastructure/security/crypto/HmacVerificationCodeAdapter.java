package com.finanscore.motorscoring.infrastructure.security.crypto;

import com.finanscore.motorscoring.application.security.port.out.VerificationCodePort;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class HmacVerificationCodeAdapter implements VerificationCodePort {
    private final byte[] pepper;
    private final SecureRandom random = new SecureRandom();

    public HmacVerificationCodeAdapter(String pepper) {
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    @Override public String generate() { return "%06d".formatted(random.nextInt(1_000_000)); }

    @Override
    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean matches(String code, String hash) {
        return MessageDigest.isEqual(
            hash(code).getBytes(StandardCharsets.UTF_8),
            hash.getBytes(StandardCharsets.UTF_8));
    }
}
