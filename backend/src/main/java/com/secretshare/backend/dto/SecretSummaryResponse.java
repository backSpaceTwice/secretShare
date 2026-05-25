package com.secretshare.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SecretSummaryResponse {

    private String shareUrl;
    private UUID token;
    private int maxUses;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;

    public SecretSummaryResponse() {}

    public SecretSummaryResponse(String shareUrl, UUID token, int maxUses, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.shareUrl = shareUrl;
        this.token = token;
        this.maxUses = maxUses;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }

    public UUID getToken() { return token; }
    public void setToken(UUID token) { this.token = token; }

    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
