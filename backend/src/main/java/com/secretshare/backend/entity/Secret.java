package com.secretshare.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="secrets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Secret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID )
    @Column(unique = true, nullable = false)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID token = UUID.randomUUID();

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @Column(name = "uses_left", nullable = false)
    private int usesLeft;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Returns true if this secret can still be viewed.
     * Checks both uses_left > 0 and that the expiry hasn't passed.
     */
    public boolean isAccessible() {
        if (usesLeft <= 0) return false;
        if (expiresAt != null && OffsetDateTime.now().isAfter(expiresAt)) return false;
        return true;
    }

    /**
     * Decrements the use counter by one.
     * The caller (SecretService) is responsible for saving the entity after
     * calling this — this method only mutates the in-memory object.
     */
    public void consumeOneUse() {
        if (usesLeft <= 0) {
            throw new IllegalStateException("No uses remaining");
        }
        this.usesLeft--;
    }
}
