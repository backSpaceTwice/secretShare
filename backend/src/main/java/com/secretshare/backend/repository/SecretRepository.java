package com.secretshare.backend.repository;

import com.secretshare.backend.entity.Secret;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<Secret, UUID> {
    Optional<Secret> findByToken(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Secret s WHERE s.token = :token")
    Optional<Secret> findByTokenForUpdate(UUID token);

    @Modifying
    @Query("DELETE FROM Secret s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    int deleteExpiredSecrets(OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM Secret s WHERE s.usesLeft = 0 AND s.createdAt < :cutoff")
    int deleteConsumedSecretsBefore(OffsetDateTime cutoff);
}
