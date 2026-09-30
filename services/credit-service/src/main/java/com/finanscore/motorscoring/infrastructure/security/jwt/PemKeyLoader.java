package com.finanscore.motorscoring.infrastructure.security.jwt;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

public final class PemKeyLoader {
    private PemKeyLoader() {}

    public static RSAPrivateKey privateKey(String location) {
        try {
            String pem = Files.readString(Path.of(location))
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar RSA private key", e);
        }
    }

    public static RSAPublicKey publicKey(String location) {
        try {
            String pem = Files.readString(Path.of(location))
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar RSA public key", e);
        }
    }
}
