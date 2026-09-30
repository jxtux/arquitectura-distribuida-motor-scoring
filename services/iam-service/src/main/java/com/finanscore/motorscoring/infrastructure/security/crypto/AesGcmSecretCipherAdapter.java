package com.finanscore.motorscoring.infrastructure.security.crypto;

import com.finanscore.motorscoring.application.security.port.out.SecretCipherPort;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

public final class AesGcmSecretCipherAdapter implements SecretCipherPort {
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretCipherAdapter(String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("IAM_TOTP_AES_KEY debe representar exactamente 32 bytes.");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    @Override
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto TOTP.", e);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            byte[] in = Base64.getDecoder().decode(encrypted);
            byte[] iv = Arrays.copyOfRange(in, 0, 12);
            byte[] data = Arrays.copyOfRange(in, 12, in.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el secreto TOTP.", e);
        }
    }
}
