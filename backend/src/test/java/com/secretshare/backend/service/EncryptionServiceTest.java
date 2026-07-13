package com.secretshare.backend.service;

import com.secretshare.backend.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(encryptionService, "encryptionKeyBase64", key);
        encryptionService.init();
    }

    @Test
    void encryptsAndDecryptsUnicodeText() {
        String plaintext = "correct horse battery staple 🔐";

        String encrypted = encryptionService.encrypt(plaintext);

        assertThat(encrypted).doesNotContain(plaintext);
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void usesANewInitializationVectorForEveryEncryption() {
        String first = encryptionService.encrypt("same value");
        String second = encryptionService.encrypt("same value");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsTamperedCiphertext() {
        byte[] encrypted = Base64.getDecoder().decode(encryptionService.encrypt("classified"));
        encrypted[encrypted.length - 1] ^= 1;

        assertThatThrownBy(() -> encryptionService.decrypt(Base64.getEncoder().encodeToString(encrypted)))
                .isInstanceOf(EncryptionException.class)
                .hasMessage("Decryption failed");
    }

    @Test
    void rejectsMissingEncryptionKey() {
        EncryptionService missingKeyService = new EncryptionService();
        ReflectionTestUtils.setField(missingKeyService, "encryptionKeyBase64", "");

        assertThatThrownBy(missingKeyService::init)
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("ENCRYPTION_KEY is not set");
    }
}
