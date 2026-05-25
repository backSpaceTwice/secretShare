package com.secretshare.backend.service;

import com.secretshare.backend.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_SIZE = 256;

    private SecretKey key;

    @Value("${app.encryption.key:}")
    private String encryptionKeyBase64;

    @PostConstruct
    void init() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            log.warn("ENCRYPTION_KEY not set — generating ephemeral AES-256 key. Secrets will be lost on restart.");
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE);
                key = keyGen.generateKey();
            } catch (Exception e) {
                throw new EncryptionException("Failed to generate ephemeral AES key", e);
            }
        } else {
            try {
                byte[] decoded = Base64.getDecoder().decode(encryptionKeyBase64.trim());
                if (decoded.length != 32) {
                    throw new EncryptionException("Encryption key must decode to 32 bytes (256-bit), got " + decoded.length + " bytes");
                }
                key = new SecretKeySpec(decoded, "AES");
                log.info("Encryption key loaded successfully");
            } catch (IllegalArgumentException e) {
                throw new EncryptionException("Encryption key is not valid Base64", e);
            }
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] plainBytes = plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plainBytes);

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            if (combined.length < IV_LENGTH) {
                throw new EncryptionException("Encrypted value is too short");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }
}
