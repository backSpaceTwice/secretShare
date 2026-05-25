# SecretShare — AGENTS.md

## Build & Run

```bash
./mvnw clean install          # full build (parent + backend)
./mvnw spring-boot:run -pl backend  # run dev server
./mvnw test -pl backend             # all tests
./mvnw test -pl backend -Dtest=BackendApplicationTests  # single test
```

- Java 21, Maven 3.9.15 (wrapper), Spring Boot 3.5.14
- Lombok annotation processing configured in `maven-compiler-plugin` (not just `spring-boot-maven-plugin`)

## Architecture

- Multi-module Maven: parent `pom.xml` + `backend` module
- Entrypoint: `com.secretshare.backend.BackendApplication`
- Stack: Spring Data JPA + Spring Web + PostgreSQL + Flyway (declared but **no migrations exist** — schema is managed via `spring.jpa.hibernate.ddl-auto=update`)
- DB connection: `jdbc:postgresql://localhost:5432/secretshare`, credentials from `$USER` / `$PASSWORD` env vars

## Known Issues (fix before adding features)

1. **Package inconsistency**: `EncryptionService` and `SecretService` are in `package service` (bare), not under `com.secretshare.backend`. Spring component-scan from `com.secretshare.backend` will **not** pick them up. Move to `com.secretshare.backend.service`.
2. **GlobalExceptionHandler** (`exception/GlobalExceptionHandler.java`) extends `RuntimeException` instead of being a `@ControllerAdvice` class — currently useless as an exception handler.
3. **Stub classes**: `SecretController`, `SecretDto`, `EncryptionService`, `SecretService` are empty shells with no real logic.

## Database

- Entity: `Secret` (table `secrets`) with fields `id` (UUID PK), `token` (UUID, unique), `encrypted_value` (TEXT), `uses_left` (int), `expires_at` (timestamptz), `created_at` (timestamptz)
- Repository custom queries: `deleteExpiredSecrets(now)` and `deleteConsumedSecretsBefore(cutoff)` — both `@Modifying` DELETE, need to run in a transaction

## Test

- Single `@SpringBootTest` context-loads test in `com.secretshare.backend` package — requires a running PostgreSQL to pass.
