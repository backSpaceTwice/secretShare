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
- Frontend: React 18 + TypeScript + Vite + react-router-dom
- Lombok annotation processing configured in `maven-compiler-plugin` (not just `spring-boot-maven-plugin`)

## Architecture

- Multi-module Maven: parent `pom.xml` + `backend` module
- Entrypoint: `com.secretshare.backend.BackendApplication`
- Stack: Spring Data JPA + Spring Web + PostgreSQL + Flyway (declared but **no migrations exist** — schema is managed via `spring.jpa.hibernate.ddl-auto=update`)
- DB connection: `jdbc:postgresql://localhost:5432/secretshare`, credentials from `$USER` / `$PASSWORD` env vars
- Frontend proxies `/api` to `localhost:8080` via Vite dev server (no CORS needed)

## Setup

1. Create the database: `createdb secretshare`
2. If PostgreSQL uses `scram-sha-256` auth and `$PASSWORD` is unset, switch to `trust` in `pg_hba.conf` for local connections, then `brew services restart postgresql@18`
3. Start backend in terminal 1, frontend in terminal 2

## Database

- Entity: `Secret` (table `secrets`) with fields `id` (UUID PK), `token` (UUID, unique), `encrypted_value` (TEXT), `uses_left` (int), `expires_at` (timestamptz), `created_at` (timestamptz)
- Repository custom queries: `deleteExpiredSecrets(now)` and `deleteConsumedSecretsBefore(cutoff)` — both `@Modifying` DELETE, need to run in a transaction

## Known Issues

- `spring.datasource.password=${PASSWORD}` — if `$PASSWORD` env var is unset, PostgreSQL must use `trust` auth for local connections or the backend won't start

## Test

- Single `@SpringBootTest` context-loads test in `com.secretshare.backend` package — requires a running PostgreSQL to pass.
