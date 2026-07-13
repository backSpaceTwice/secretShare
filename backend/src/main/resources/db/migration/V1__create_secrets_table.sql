CREATE TABLE IF NOT EXISTS secrets (
    id UUID NOT NULL,
    token UUID NOT NULL,
    encrypted_value TEXT NOT NULL,
    uses_left INTEGER NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_secrets PRIMARY KEY (id),
    CONSTRAINT uk_secrets_token UNIQUE (token),
    CONSTRAINT ck_secrets_uses_left_nonnegative CHECK (uses_left >= 0)
);

CREATE INDEX IF NOT EXISTS idx_secrets_expires_at
    ON secrets (expires_at)
    WHERE expires_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_secrets_consumed_created_at
    ON secrets (created_at)
    WHERE uses_left = 0;
