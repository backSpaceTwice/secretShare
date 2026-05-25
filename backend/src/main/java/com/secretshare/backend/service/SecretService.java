package com.secretshare.backend.service;

import com.secretshare.backend.dto.SecretSummaryResponse;
import com.secretshare.backend.dto.SecretValueResponse;
import com.secretshare.backend.entity.Secret;
import com.secretshare.backend.exception.SecretNotFoundException;
import com.secretshare.backend.repository.SecretRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecretService {

    private final SecretRepository secretRepository;
    private final EncryptionService encryptionService;

    public SecretSummaryResponse createSecret(String value, int maxUses, Integer ttlHours) {
        if (maxUses < 1) maxUses = 1;
        if (maxUses > 100) maxUses = 100;

        if (ttlHours == null) ttlHours = 24;
        if (ttlHours < 1) ttlHours = 1;
        if (ttlHours > 8760) ttlHours = 8760;

        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(ttlHours);

        String encryptedValue = encryptionService.encrypt(value);

        Secret secret = Secret.builder()
                .encryptedValue(encryptedValue)
                .usesLeft(maxUses)
                .expiresAt(expiresAt)
                .build();

        secret = secretRepository.save(secret);

        return new SecretSummaryResponse(null, secret.getToken(), secret.getUsesLeft(),
                secret.getExpiresAt(), secret.getCreatedAt());
    }

    @Transactional
    public SecretValueResponse viewSecret(UUID token) {
        Secret secret = secretRepository.findByToken(token)
                .orElseThrow(SecretNotFoundException::new);

        if (!secret.isAccessible()) {
            throw new SecretNotFoundException();
        }

        secret.consumeOneUse();
        secretRepository.save(secret);

        String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());

        return new SecretValueResponse(decryptedValue, secret.getUsesLeft());
    }
}
