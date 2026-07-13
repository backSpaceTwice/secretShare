package com.secretshare.backend.service;

import com.secretshare.backend.dto.SecretValueResponse;
import com.secretshare.backend.entity.Secret;
import com.secretshare.backend.repository.SecretRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SecretServiceTest {

    @Test
    void viewSecretUsesLockedLookupAndDeletesFinalUse() {
        UUID token = UUID.randomUUID();
        Secret secret = Secret.builder()
                .token(token)
                .encryptedValue("encrypted")
                .usesLeft(1)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();

        AtomicBoolean lockedLookupCalled = new AtomicBoolean();
        AtomicBoolean deleteCalled = new AtomicBoolean();

        SecretRepository secretRepository = (SecretRepository) Proxy.newProxyInstance(
                SecretRepository.class.getClassLoader(),
                new Class<?>[]{SecretRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByTokenForUpdate" -> {
                        assertThat(args[0]).isEqualTo(token);
                        lockedLookupCalled.set(true);
                        yield Optional.of(secret);
                    }
                    case "delete" -> {
                        assertThat(args[0]).isSameAs(secret);
                        deleteCalled.set(true);
                        yield null;
                    }
                    default -> throw new AssertionError("Unexpected repository call: " + method.getName());
                });

        EncryptionService encryptionService = new EncryptionService() {
            @Override
            public String decrypt(String encryptedBase64) {
                assertThat(encryptedBase64).isEqualTo("encrypted");
                return "plaintext";
            }
        };

        SecretService secretService = new SecretService(secretRepository, encryptionService);

        SecretValueResponse response = secretService.viewSecret(token);

        assertThat(response.getValue()).isEqualTo("plaintext");
        assertThat(response.getUsesLeft()).isZero();
        assertThat(lockedLookupCalled).isTrue();
        assertThat(deleteCalled).isTrue();
    }
}
