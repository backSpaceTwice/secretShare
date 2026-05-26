package com.secretshare.backend.service;

import com.secretshare.backend.repository.SecretRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class CleanupTask {

    private static final Logger log = LoggerFactory.getLogger(CleanupTask.class);
    private static final int CONSUMED_CUTOFF_DAYS = 7;

    private final SecretRepository secretRepository;

    @Transactional
    @Scheduled(cron = "${cleanup.cron}")
    public void cleanup() {
        OffsetDateTime now = OffsetDateTime.now();

        int expiredDeleted = secretRepository.deleteExpiredSecrets(now);
        if (expiredDeleted > 0) {
            log.info("Deleted {} expired secrets", expiredDeleted);
        }

        OffsetDateTime cutoff = now.minusDays(CONSUMED_CUTOFF_DAYS);
        int consumedDeleted = secretRepository.deleteConsumedSecretsBefore(cutoff);
        if (consumedDeleted > 0) {
            log.info("Deleted {} fully-consumed secrets older than {} days", consumedDeleted, CONSUMED_CUTOFF_DAYS);
        }

        if (expiredDeleted == 0 && consumedDeleted == 0) {
            log.debug("Cleanup completed — nothing to delete");
        }
    }
}
