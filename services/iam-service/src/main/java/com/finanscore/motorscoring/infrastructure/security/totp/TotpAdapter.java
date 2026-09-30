package com.finanscore.motorscoring.infrastructure.security.totp;

import com.finanscore.motorscoring.application.security.port.out.TotpPort;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;

public final class TotpAdapter implements TotpPort {
    private static final int PERIOD_SECONDS = 30;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TotpAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String generateSecret() {
        byte[] data = new byte[20];
        random.nextBytes(data);
        return Base32.encode(data);
    }

    @Override
    public String buildOtpAuthUri(
        String issuer,
        String accountLabel,
        String secret) {

        String label =
            enc(issuer) + ":" + enc(accountLabel);

        return "otpauth://totp/" + label
            + "?secret=" + secret
            + "&issuer=" + enc(issuer)
            + "&algorithm=SHA1"
            + "&digits=6"
            + "&period=30";
    }

    @Override
    public boolean verify(
        String secret,
        String code) {

        if (code == null
            || !code.matches("\\d{6}")) {
            return false;
        }

        long window =
            clock.instant().getEpochSecond()
            / PERIOD_SECONDS;

        /*
         * Tolerancia ±1 ventana:
         * anterior / actual / siguiente.
         * Cada ventana dura 30 segundos.
         */
        for (long delta = -1;
             delta <= 1;
             delta++) {

            if (generate(
                secret,
                window + delta
            ).equals(code)) {
                return true;
            }
        }

        return false;
    }

    String generate(
        String secret,
        long counter) {

        try {
            byte[] key =
                Base32.decode(secret);

            byte[] message =
                ByteBuffer
                    .allocate(8)
                    .putLong(counter)
                    .array();

            Mac mac =
                Mac.getInstance("HmacSHA1");

            mac.init(
                new SecretKeySpec(
                    key,
                    "HmacSHA1"));

            byte[] hmac =
                mac.doFinal(message);

            int offset =
                hmac[hmac.length - 1]
                & 0x0f;

            int binary =
                ((hmac[offset] & 0x7f) << 24)
                | ((hmac[offset + 1] & 0xff) << 16)
                | ((hmac[offset + 2] & 0xff) << 8)
                | (hmac[offset + 3] & 0xff);

            return "%06d".formatted(
                binary % 1_000_000);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(
        String value) {

        return URLEncoder
            .encode(
                value,
                StandardCharsets.UTF_8)
            .replace("+", "%20");
    }
}
