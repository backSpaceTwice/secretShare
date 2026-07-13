# SecretShare

A self-hosted, one-time secret sharing app. Paste a secret, get a shareable link — the secret is encrypted at rest and automatically destroyed after it has been viewed the configured number of times or its TTL expires.

## How it works

1. A sender pastes their secret and configures a max-view count (1–100) and expiry window (1–8760 hours).
2. The backend encrypts the value with AES-256-GCM and stores it alongside a random UUID token.
3. The sender shares the generated link with the recipient.
4. When the recipient opens the link they see a confirmation page warning them that viewing will consume one use. They must click **Reveal Secret** to proceed.
5. Once confirmed, the backend decrypts and returns the value, then decrements the use counter. Once uses reach zero the record is deleted immediately; if a TTL was set it is also purged by the nightly cleanup job.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring Data JPA, Flyway, PostgreSQL |
| Encryption | AES-256-GCM (per-message IV, 128-bit auth tag) |
| Frontend | React 19, TypeScript, Vite, React Router |
| Build | Maven (multi-module) |

## Project structure

```
secretShare/
├── backend/          # Spring Boot application
│   └── src/main/java/com/secretshare/backend/
│       ├── config/       SecurityConfig, RateLimitInterceptor, WebConfig
│       ├── controller/   SecretController.java
│       ├── dto/          request/response objects
│       ├── entity/       Secret.java (JPA entity)
│       ├── exception/    GlobalExceptionHandler.java
│       ├── repository/   SecretRepository.java
│       └── service/      SecretService, EncryptionService, CleanupTask
└── frontend/         # Vite + React SPA
    └── src/
        ├── api.ts        typed fetch wrappers
        └── pages/        CreateSecret.tsx, ViewSecret.tsx
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 20+ / npm
- PostgreSQL 14+

## Getting started

### 1. Database

Create a database:

```sql
CREATE DATABASE secretshare;
```

Flyway applies the versioned migrations in `backend/src/main/resources/db/migration` when the backend starts. Hibernate validates the resulting schema but does not modify it. Existing databases created by earlier versions are baselined at version 0 before the first migration is applied.

### 2. Environment variables

| Variable | Required | Description |
|---|---|---|
| `USER` | Yes | PostgreSQL username |
| `PASSWORD` | Yes | PostgreSQL password |
| `ENCRYPTION_KEY` | **Required** | Base64-encoded 256-bit AES key. The app will not start without this. |
| `ALLOWED_ORIGINS` | No | Comma-separated list of allowed CORS origins. Defaults to `http://localhost:5173`. Set to your frontend domain in production. |
| `TRUST_FORWARDED_HEADERS` | No | Set to `true` only when the backend is reachable exclusively through a trusted reverse proxy that overwrites `X-Forwarded-For`. Defaults to `false`. |

Generate a key:

```bash
openssl rand -base64 32
```

### 3. Run the backend

```bash
USER=youruser PASSWORD=yourpassword ENCRYPTION_KEY=<base64key> \
  ./mvnw spring-boot:run -pl backend
```

The API listens on `http://localhost:8080`.

### 4. Run the frontend (dev)

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api/*` to `http://localhost:8080`, so the app is available at `http://localhost:5173`.

### 5. Production build

```bash
cd frontend
npm run build   # outputs to frontend/dist/
```

Serve `frontend/dist/` as static files behind the same origin as the backend, or configure your reverse proxy to route `/api/*` to the Spring Boot process.

## API reference

### Create a secret

```
POST /api/secrets
Content-Type: application/json   (required — returns 415 if omitted)

{
  "value":    "my secret text",
  "maxUses":  1,        // optional, default 1, max 100
  "ttlHours": 24        // optional, default 24, max 8760 (1 year)
}
```

**Response 201**

```json
{
  "token":     "550e8400-e29b-41d4-a716-446655440000",
  "shareUrl":  "http://host/api/secrets/550e8400-...",
  "maxUses":   1,
  "expiresAt": "2026-06-13T00:00:00Z",
  "createdAt": "2026-06-12T00:00:00Z"
}
```

**Rate limit:** 10 requests per minute per IP. Exceeding this returns `429 Too Many Requests`.

### View a secret

```
GET /api/secrets/{token}
```

**Response 200** — returns the decrypted value and decrements the use counter.

```json
{
  "value":    "my secret text",
  "usesLeft": 0
}
```

**Response 404** — secret does not exist, has expired, or has no uses remaining.

**Rate limit:** 30 requests per minute per IP.

## Security

### Encryption

Secrets are encrypted with **AES-256-GCM** before being written to the database. A fresh 12-byte IV is generated for every encryption operation and prepended to the ciphertext. The authentication tag (128-bit) ensures ciphertext integrity. Plaintext is never persisted.

### Encryption key

The `ENCRYPTION_KEY` environment variable is **required** — the application will refuse to start without it. The decoded key bytes are zeroed from memory immediately after the `SecretKey` object is constructed.

To generate a suitable key:

```bash
openssl rand -base64 32
```

Store this value in a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.) rather than in shell history or `.env` files.

Keep the key stable for the lifetime of every stored secret. Changing or losing it makes existing records impossible to decrypt. Automatic key rotation is not currently implemented; a production rotation design should store a key identifier with each record and decrypt through a KMS or equivalent key-management service.

### Rate limiting

A fixed-window rate limiter (per client IP) protects both endpoints:

| Endpoint | Limit |
|---|---|
| `POST /api/secrets` | 10 requests / minute |
| `GET /api/secrets/{token}` | 30 requests / minute |

Exceeded requests receive `429 Too Many Requests`.

By default, the limiter uses the direct network peer address and ignores client-supplied forwarding headers. Enable `TRUST_FORWARDED_HEADERS` only behind a trusted proxy. Expired counters are removed and the number of tracked clients is capped to prevent unbounded memory growth.

The limiter is local to one backend process. Deployments with multiple backend instances should enforce limits at a shared API gateway or replace this implementation with a shared store such as Redis.

### HTTP security headers

Every response includes the following headers via Spring Security:

| Header | Value |
|---|---|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | `default-src 'self'` |

### CORS

Cross-origin requests are restricted to the origins listed in `ALLOWED_ORIGINS`. Only `GET` and `POST` methods and the `Content-Type` header are permitted.

### Token design

Each secret is identified by a randomly generated UUID v4 (122 bits of entropy). Tokens are not sequential and cannot be guessed without the rate limiter being defeated first.

### Secret lifetime

Once a secret's last use is consumed, the database record is deleted immediately — it does not linger. Expired secrets that still have uses remaining are purged by the nightly cleanup job.

Reveal operations acquire a PostgreSQL row lock for the duration of the transaction. Concurrent requests for a secret's final use are serialized, so only one request receives the plaintext.

### Audit logging

All secret access events are logged server-side using the first 8 characters of the token (enough to correlate events, not enough to replay a request):

- Successful access: token prefix, uses remaining
- Denied access: token prefix, reason (not found / expired / consumed)

## Configuration

All tunable properties live in `backend/src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/secretshare` | JDBC URL |
| `app.encryption.key` | *(none — required)* | Base64 AES-256 key |
| `app.cors.allowed-origins` | `http://localhost:5173` | Comma-separated allowed CORS origins |
| `app.proxy.trust-forwarded-headers` | `false` | Whether to trust `X-Forwarded-For` from the network peer |
| `cleanup.cron` | `0 0 3 * * ?` | Cron expression for the nightly cleanup job |

## Tests

Run the self-contained backend suite from the repository root:

```bash
./mvnw test -pl backend
```

The suite covers encryption round trips and tamper detection, API validation and error responses, rate-limit and proxy behavior, and the locked final-use service path. A live PostgreSQL smoke test is still recommended in deployment CI to execute Flyway and verify the database-specific row-lock behavior.
