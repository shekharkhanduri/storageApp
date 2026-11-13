# StoreIt Backend

Java + Spring Boot 3 REST API for the StoreIt storage platform.
Handles user auth (JWT), file metadata (PostgreSQL + Flyway), and API docs (Swagger).
File bytes live in object storage; this service only stores metadata.

**Stack:** Java 21 · Spring Boot 3.3.5 · Spring Security + JWT (jjwt) · Spring Data JPA ·
PostgreSQL 16 · Flyway · Lombok + MapStruct · springdoc-openapi

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21 | Project targets `java.version=21` (`pom.xml`). Newer JDKs (e.g. 25) fail compilation (Lombok) — use JDK 21. |
| Maven | — | Wrapper `./mvnw` is included, no install needed |
| Docker + Docker Compose | recent | Runs PostgreSQL locally |

Check yours:

```bash
java -version   # needs 21.x
docker --version
```

## Quick start
    
```bash
cd backend

# 1. Start PostgreSQL + MinIO (Docker or Podman Compose)
docker compose up -d
# Fedora/Podman: podman-compose up -d

# 2. Build (runs tests) with the bundled JDK
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw clean package

# 3. Run
JAVA_HOME=$PWD/.tools/jdk-21 java -jar target/storeit-backend-0.0.1-SNAPSHOT.jar
# or in dev (no separate build step):
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw spring-boot:run
```

API: `http://localhost:8080` · Swagger UI: `http://localhost:8080/swagger-ui.html`
MinIO console: `http://localhost:9001` (`minioadmin` / `minioadmin`).

On Windows use `mvnw.cmd` instead of `./mvnw`. The bundled JDK lives at
`backend/.tools/jdk-21`; system Java must be 21 (Lombok rejects newer JDKs).

## Build commands

| Command | What it does |
|---------|--------------|
| `./mvnw clean package` | Full build: compile + run tests + package runnable jar into `target/` |
| `./mvnw clean package -DskipTests` | Fast build: same, but skips tests |
| `./mvnw spring-boot:run` | Build + run the app in place (dev mode, no jar needed) |
| `java -jar target/storeit-backend-0.0.1-SNAPSHOT.jar` | Run the packaged jar |
| `./mvnw test` | Run tests only |
| `./mvnw clean` | Remove `target/` build output |

## Configuration

Defaults live in `src/main/resources/application.yml` (port `8080`, datasource
`jdbc:postgresql://localhost:5432/storeit`, JWT expiries). Override without editing
the file via environment variables, e.g.:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/storeit \
SPRING_DATASOURCE_USERNAME=storeit \
SPRING_DATASOURCE_PASSWORD=storeit \
java -jar target/storeit-backend-0.0.1-SNAPSHOT.jar
```

DB schema is managed by Flyway (`src/main/resources/db/migration/`) —
`ddl-auto: validate`, so never create tables by hand; add a migration instead.

## API overview

Auth (`/api/auth`): `POST /register`, `POST /login`, `POST /logout`,
`POST /refresh`, `GET /me`.

Files (`/api/files`):

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/upload-url` | Create PENDING record + presigned PUT URL |
| `POST` | `/{id}/confirm` | Verify object in MinIO and mark ACTIVE |
| `GET` | `/` | List files (`type`, `search`, `sort`, `order`, `page`, `size`) |
| `GET` | `/{id}` | Get one file |
| `PATCH` | `/{id}/rename` | Rename |
| `DELETE` | `/{id}` | Delete from MinIO + PostgreSQL |
| `GET` | `/{id}/download-url` | Presigned GET URL |
| `PATCH` | `/{id}/share` | Set shared emails (empty list clears shares) |
| `GET` | `/space` | `{ usedBytes, totalBytes }` |

`type` accepts one value or a comma-separated list, e.g. `/api/files?type=video,audio`.
Every `FileDto` includes a presigned `url`, `ownerName`, `ownerEmail`, and
`storageBackend: "minio"` so the frontend can render MinIO files with the same
components as Appwrite files. Listing returns files the caller owns **or** that
were shared with their email.

See Swagger UI for the full contract and schemas.

## Tests

```bash
# Unit tests (default; Testcontainers tests excluded)
./mvnw test

# Integration tests (requires a running Docker daemon)
./mvnw test -Dgroups=integration -DexcludedGroups=
```

## Frontend wiring

The Next.js app (repo root) talks to this backend at:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Project structure

```
src/main/java/com/storeit/
├── StoreItApplication.java
├── config/         # Security + CORS + JWT filter wiring
├── controller/     # AuthController, ...
├── service/        # UserService, ...
├── repository/     # Spring Data JPA repositories
├── entity/         # User, ... (@Entity)
├── dto/  mapper/   # Request/response DTOs + MapStruct mappers
├── security/       # JwtUtil, JwtAuthFilter
└── exception/      # GlobalExceptionHandler, ...
src/main/resources/
├── application.yml
└── db/migration/   # Flyway SQL migrations (V1__..., V2__...)
```
