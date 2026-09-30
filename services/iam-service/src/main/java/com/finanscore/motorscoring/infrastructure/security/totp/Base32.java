package com.finanscore.motorscoring.infrastructure.security.totp;

final class Base32 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private Base32() {}

    static String encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) out.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        return out.toString();
    }

    static byte[] decode(String input) {
        String s = input.replace("=", "").replace(" ", "").toUpperCase();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0, bitsLeft = 0;
        for (char c : s.toCharArray()) {
            int val = ALPHABET.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Base32 inválido");
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
