package com.secretshare.backend.repository;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecretRepositoryTest {

    @Test
    void revealLookupUsesPessimisticWriteLock() throws NoSuchMethodException {
        Lock lock = SecretRepository.class
                .getMethod("findByTokenForUpdate", UUID.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
