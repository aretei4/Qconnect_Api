package com.api.distr.docs.mobile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts/decrypts short payloads for use inside URLs (AES-256-GCM).
 *
 * Output format:  base64url( IV[12] + ciphertext + tag )
 * Key           : SHA-256 of app.url.secret (falls back to onlyoffice.jwt.secret),
 *                 so any secret string works regardless of length.
 */
@Service
public class UrlCryptoService {

    private static final int IV_LEN  = 12;   // GCM standard nonce length
    private static final int TAG_LEN = 128;  // auth tag bits

    private final SecureRandom random = new SecureRandom();

    @Value("${app.url.secret:${onlyoffice.jwt.secret}}")
    private String secret;

    private SecretKeySpec key() throws Exception {
        byte[] k = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }

    /** Encrypts plain text → URL-safe token. */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LEN, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv,  0, out, 0,         iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("URL encryption failed: " + e.getMessage(), e);
        }
    }

    /** Decrypts a token produced by {@link #encrypt}. Throws on tampered/invalid input. */
    public String decrypt(String token) {
        try {
            byte[] in = Base64.getUrlDecoder().decode(token);
            if (in.length <= IV_LEN) throw new IllegalArgumentException("token too short");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(),
                    new GCMParameterSpec(TAG_LEN, in, 0, IV_LEN));
            byte[] plain = cipher.doFinal(in, IV_LEN, in.length - IV_LEN);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or tampered URL token", e);
        }
    }
}
