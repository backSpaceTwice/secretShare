# SecretShare

A self-hosted, one-time secret sharing app. Paste a secret, get a shareable link — the secret is encrypted at rest and automatically destroyed after it has been viewed the configured number of times or its TTL expires.

## How it works

1. A sender pastes their secret and configures a max-view count (1–100) and expiry window (1–8760 hours).
2. The backend encrypts the value with AES-256-GCM and stores it alongside a random UUID token.
3. The sender shares the generated link with the recipient.
4. When the recipient opens the link, the backend decrypts and returns the value, then decrements the use counter. Once uses reach zero or the TTL passes, the secret is gone.
5. A nightly cleanup job (3 AM by default) purges expired secrets and consumed secrets older than 7 days.

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

Create a database and user:

```sql
CREATE DATABASE secretshare;
```

### 2. Environment variables

| Variable | Required | Description |
|---|---|---|
| `USER` | Yes | PostgreSQL username |
| `PASSWORD` | Yes | PostgreSQL password |
| `ENCRYPTION_KEY` | Recommended | Base64-encoded 256-bit AES key (32 raw bytes). If omitted, an ephemeral key is generated and **all secrets are lost on restart**. |

Generate a key:

```bash
openssl rand -base64 32
```

### 3. Run the backend

```bash
cd backend
USER=youruser PASSWORD=yourpassword ENCRYPTION_KEY=<base64key> ./mvnw spring-boot:run
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
Content-Type: application/json

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
  "usesLeft":  1,
  "expiresAt": "2026-06-13T00:00:00Z",
  "createdAt": "2026-06-12T00:00:00Z"
}
```

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

## Configuration

All tunable properties live in `backend/src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/secretshare` | JDBC URL |
| `app.encryption.key` | *(empty — ephemeral)* | Base64 AES-256 key |
| `cleanup.cron` | `0 0 3 * * ?` | Cron expression for the nightly cleanup job |
