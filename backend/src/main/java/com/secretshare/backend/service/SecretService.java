package com.secretshare.backend.service;

import com.secretshare.backend.dto.SecretSummaryResponse;
import com.secretshare.backend.dto.SecretValueResponse;
import com.secretshare.backend.entity.Secret;
import com.secretshare.backend.exception.SecretNotFoundException;
import com.secretshare.backend.repository.SecretRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecretService {

    private static final Logger log = LoggerFactory.getLogger(SecretService.class);

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
        Secret secret = secretRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> {
                    log.warn("Secret access denied — not found: token={}...", token.toString().substring(0, 8));
                    return new SecretNotFoundException();
                });

        if (!secret.isAccessible()) {
            log.warn("Secret access denied — expired or consumed: token={}...", token.toString().substring(0, 8));
            throw new SecretNotFoundException();
        }

        secret.consumeOneUse();
        String decryptedValue = encryptionService.decrypt(secret.getEncryptedValue());

        if (secret.getUsesLeft() == 0) {
            secretRepository.delete(secret);
            log.info("Secret fully consumed and deleted: token={}...", token.toString().substring(0, 8));
        } else {
            secretRepository.save(secret);
            log.info("Secret accessed: token={}..., usesLeft={}", token.toString().substring(0, 8), secret.getUsesLeft());
        }

        return new SecretValueResponse(decryptedValue, secret.getUsesLeft());
    }
}
