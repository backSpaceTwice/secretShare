# SecretShare — AGENTS.md

## Build & Run

```bash
./mvnw clean install          # full build (parent + backend)
./mvnw spring-boot:run -pl backend  # run dev server
./mvnw test -pl backend             # all tests
./mvnw test -pl backend -Dtest=BackendApplicationTests  # single test

cd frontend && npm run dev    # frontend dev server at http://localhost:5173
cd frontend && npm run build  # production build
```

- Java 21, Maven 3.9.15 (wrapper), Spring Boot 3.5.14
- Frontend: React 19 + TypeScript + Vite + react-router-dom
- Lombok annotation processing configured in `maven-compiler-plugin` (not just `spring-boot-maven-plugin`)

## Architecture

- Multi-module Maven: parent `pom.xml` + `backend` module
- Entrypoint: `com.secretshare.backend.BackendApplication`
- Stack: Spring Data JPA + Spring Web + PostgreSQL + Flyway; migrations live in `backend/src/main/resources/db/migration` and Hibernate validates the schema
- DB connection: `jdbc:postgresql://localhost:5432/secretshare`, credentials from `$USER` / `$PASSWORD` env vars
- Frontend proxies `/api` to `localhost:8080` via Vite dev server (no CORS needed)

## Setup

1. Create the database: `createdb secretshare`
2. Set `$USER`, `$PASSWORD`, and a Base64-encoded 32-byte `$ENCRYPTION_KEY`
3. Start backend in terminal 1, frontend in terminal 2

## Database

- Entity: `Secret` (table `secrets`) with fields `id` (UUID PK), `token` (UUID, unique), `encrypted_value` (TEXT), `uses_left` (int), `expires_at` (timestamptz), `created_at` (timestamptz)
- Reveal lookup uses `PESSIMISTIC_WRITE` inside the service transaction so concurrent final-use requests serialize
- Repository cleanup queries `deleteExpiredSecrets(now)` and `deleteConsumedSecretsBefore(cutoff)` are `@Modifying` DELETE operations and run in a transaction

## Known Issues

- The encryption key must remain stable while secrets exist; automatic key rotation is not implemented
- Rate limiting is process-local; horizontally scaled deployments need a shared gateway or store
- Trust `X-Forwarded-For` only by setting `TRUST_FORWARDED_HEADERS=true` behind a trusted proxy that overwrites it

## Test

- `./mvnw test -pl backend` is self-contained and does not require PostgreSQL.
- Deployment CI should additionally run a PostgreSQL-backed smoke test for Flyway and row locking.
