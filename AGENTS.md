# AGENTS.md — StoreIt (Storage Management & File Sharing Platform)

## What this is
Next.js 15 (App Router) + React 19 + TypeScript + TailwindCSS + shadcn/ui app.
Auth via custom Java backend (Spring Boot JWT); file storage still on Appwrite Cloud
(Database + Storage). Google-Drive-like: upload, browse by type,
rename/share/download/delete, dashboard quota, search + sort.

## Stack
- Next.js 15.0.2, React 19 RC (`19.0.0-rc-02c0e824-20241028`), `node-appwrite@14.1.0`
  (exports only `Databases` — no `TablesDB`; file actions keep using
  `listDocuments/createDocument/...`). Auth does NOT use Appwrite SDK.
- TailwindCSS + shadcn/ui (Radix), react-hook-form + zod, recharts, use-debounce.
- Server Actions (`"use server"`) for backend/Appwrite calls;
  `experimental.serverActions.bodySizeLimit: 100MB`.

## Routes & key files
- `app/(auth)/sign-in`, `sign-up` → `components/auth/AuthForm.tsx` (email + password,
  no OTP; redirects to `/` on success)
- `app/(root)/layout.tsx` — auth guard (`getCurrentUser` or redirect),
  `components/layout/{Sidebar,MobileNavigation,Header}`, `components/files/FileUploader`,
  `Toaster`; `dynamic = force-dynamic`.
- `app/(root)/page.tsx` — Dashboard (`components/dashboard/Chart`, usage summary, recent files).
- `app/(root)/[type]/page.tsx` — type pages (`documents|images|media|others`),
  `components/search/Sort`, `components/files/Card` grid.
- `components/files/` — `FileUploader` (react-dropzone, MAX_FILE_SIZE 50MB), `Card`,
  `ActionDropdown` + `ActionsModalContent`, `Thumbnail`.
- `components/{search/{Search,Sort},common/FormattedDateTime,ui/*}`, `hooks/use-toast.ts`.
- `lib/appwrite/index.ts` — `createAdminClient` (API key) for file actions only.
  `lib/appwrite/config.ts` reads env below.
- `lib/api/client.ts` — Java backend base URL + cookie names + `apiFetch` wrapper.
- `lib/api/auth.api.ts` — raw `register/login/refresh/me/logout` backend calls.
- `lib/auth/cookies.ts` — `setAuthCookies`/`clearAuthCookies` (single place).
- `lib/actions/user.actions.ts` — thin wrappers: `createAccount`, `signInUser`,
  `getCurrentUser` (access→401→refresh→retry), `signOutUser`.
- `lib/actions/file.actions.ts` — `uploadFile`, `getFiles` (owner/shared
  `Query.or` + type/name/sort/limit), `renameFile`, `updateFileUsers` (share),
  `deleteFile`, `getTotalSpaceUsed` (2GB quota).
- `lib/utils.ts` — `getFileType`, `getFileIcon`, `convertFileSize`,
  `constructFileUrl/constructDownloadUrl`, `getUsageSummary`, `getFileTypesParams`.
- `constants/index.ts` — `navItems`, `actionsDropdownItems`, `sortTypes`,
  `avatarPlaceholderUrl`, `MAX_FILE_SIZE`.
- `types/index.d.ts` — `FileType`, `*Props` interfaces.

## Appwrite live setup (Frankfurt region)
- Endpoint: `https://fra.cloud.appwrite.io/v1`
- Project ID: `6ac51272000e9f5b42c1`
- Database ID: `6ac5131f0007a1e8bc4f`
- Users table ID: `6ac513be001775af3bb4` — attrs (4): `fullName:string(100,req)`,
  `email:email(req)`, `avatar:url(opt)`, `accountId:string(100,req)`.
- Files table ID: `6ac513fb00290921fbee` — attrs (9): `name:string(255,req)`,
  `type:string(20,req: document|image|video|audio|other)`, `url:url(2000,req)`,
  `extension:string(20,opt)`, `size:integer(min 0,req)`, `owner:string(100,req)`,
  `accountId:string(100,req)`, `users:string[](100,opt,array)`, `bucketFileId:string(100,req)`.
- Files indexes: `idx_owner (key, owner)`, `idx_name (fulltext, name)`.
- Bucket ID: `6ac514160021f44b4c47` — 50MB max, extensions `*`, File Security off.
- Auth: Email OTP enabled. Platform: `localhost:3000` registered.
- API key (`storageApp`, id `6ac52362000e1a09efe7`, `standard_...`): must include
  `databases.read/write`, `tables.read/write`, `rows.read/write`,
  `collections.read/write`, `attributes.read/write`, `documents.read/write`,
  `indexes.read/write`, `buckets.read/write`, `files.read/write`,
  `users.read/write`, `sessions.read/write`, `project.read`.
  - Console CLI is REPL (every line starts with `appwrite`, no shell vars/comments);
    `databases` is deprecated → use `tablesdb` for schema; keys via
    `appwrite project list-keys/get-key/update-key` with repeated `--scopes` flags.

## Env (`.env.local`, gitignored — never commit)
```env
NEXT_PUBLIC_APPWRITE_ENDPOINT="https://fra.cloud.appwrite.io/v1"
NEXT_PUBLIC_APPWRITE_PROJECT="6ac51272000e9f5b42c1"
NEXT_PUBLIC_APPWRITE_DATABASE="6ac5131f0007a1e8bc4f"
NEXT_PUBLIC_APPWRITE_USERS_COLLECTION="6ac513be001775af3bb4"
NEXT_PUBLIC_APPWRITE_FILES_COLLECTION="6ac513fb00290921fbee"
NEXT_PUBLIC_APPWRITE_BUCKET="6ac514160021f44b4c47"
NEXT_APPWRITE_KEY="standard_... (secret, rotate if exposed)"
```

## Commands (use system npm)
- System toolchain: `/usr/bin/node` (v24) + `/usr/bin/npm` (v11). Do NOT use
  `~/.local/bin/npm` (stale 12.x, corrupts `next` install).
- Install: `/usr/bin/npm install` (if `node_modules/next` corrupt:
  `rm -rf node_modules/next && /usr/bin/npm install --prefer-online --no-audit --no-fund`).
- Dev: `/usr/bin/npm run dev` → `http://localhost:3000` (Turbopack; redirects to `/sign-in`).
- Build/start: `/usr/bin/npm run build`, `/usr/bin/npm start`. Lint: `/usr/bin/npm run lint`
  (build ignores TS/ESLint per `next.config.ts`).
- Always `rm -rf .next` after `.env.local` or `next.config.ts` changes.

## Local gotchas (already applied — do not revert)
- `package.json` has NO `"type"`/`"main"`/`"directories"` (a `"type":"commonjs"` breaks
  ESM `import/export` in dev).
- `next.config.ts` `images.remotePatterns` includes `cdn.pixabay.com`,
  `img.freepik.com`, `cloud.appwrite.io`, AND `fra.cloud.appwrite.io`.
- `components/FileUploader.tsx` GIF `Image` uses `unoptimized` (animated `file-loader.gif`).

## Smoke test
1. `/sign-up` → email → OTP modal → verify → `/` dashboard (empty chart first run).
2. Upload <50MB file → appears in recent + type page; test rename/share/download/delete.
3. Search `?query=`, sort `?sort=`, visit `/documents|/images|/media|/others`.
4. Sign out → `/sign-in` with same email OTP-logins.
- Direct key check (no email sent):
  `node -e "listDocuments(users/files, limit 1)"` must print `USERS_OK`/`FILES_OK`,
  never `missing scopes`.

## Backend Direction (simple)
- Goal is a learning-focused personal cloud/file-storage system (Drive-like concept,
  not a Drive clone). Main focus is the Java backend; the current Next.js app is the
  starter frontend — no rebuild from scratch.
- Backend: Java + Spring Boot (REST APIs, auth, file metadata, presigned-URL
  coordination) + Spring Security + Spring Data JPA/Hibernate + PostgreSQL.
- Auth basics: user registration, login/logout; every file has an owner
  (`users`: id, email, password_hash, created_at).
- Storage basics: file bytes in local MinIO via S3 API (AWS SDK for Java v2);
  metadata in PostgreSQL (`files`: id, owner_id, filename, object_key, size,
  content_type, status PENDING/ACTIVE/FAILED, created_at).
- Flow: browser requests upload → backend creates PENDING record + presigned PUT URL →
  browser uploads directly to MinIO → backend verifies/marks ACTIVE; downloads via
  presigned GET URL after authorization check. Basic file listing, rename, delete.

## Architecture (Full Design)

### Overview
Three-tier system: Next.js frontend → Java REST backend → PostgreSQL + MinIO.
The backend **never streams file bytes** — it only issues presigned URLs; browsers
talk directly to MinIO for all uploads and downloads.

### Tech Stack (Backend)
- Java 21 (LTS) + Spring Boot 3.x
- Spring Security 6 — JWT filter (jjwt library); BCrypt password hashing
- Spring Data JPA + Hibernate — ORM over PostgreSQL 16
- AWS SDK for Java v2 — S3Client pointed at local MinIO
- MinIO — local object storage (Docker); bucket `storeit-files`,
  path pattern `{owner_id}/{uuid}-{filename}`
- Lombok — eliminates entity/DTO boilerplate (`@Data @Builder @NoArgsConstructor`)
- MapStruct — compile-time entity→DTO mapping (no manual getters/setters)
- springdoc-openapi — auto Swagger UI at `localhost:8080/swagger-ui.html`
- Flyway — mandatory DB migrations under `src/main/resources/db/migration/`
- Testcontainers — integration tests against real PostgreSQL + MinIO instances
- Docker Compose — spins up PostgreSQL + MinIO for local dev

### Backend Project Structure
```
backend/
├── src/main/java/com/storeit/
│   ├── StoreItApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java      # Spring Security + CORS + JWT filter wiring
│   │   ├── S3Config.java            # S3Client bean → MinIO endpoint
│   │   └── AppConfig.java           # general beans, scheduled tasks
│   ├── controller/
│   │   ├── AuthController.java      # /api/auth/**
│   │   └── FileController.java      # /api/files/**
│   ├── service/
│   │   ├── UserService.java
│   │   ├── FileService.java
│   │   └── StorageService.java      # presigned PUT/GET URL generation + HeadObject
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── FileRepository.java      # Pageable queries + fulltext search
│   ├── entity/
│   │   ├── User.java                # @Entity users table
│   │   └── FileMetadata.java        # @Entity files table
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── AuthResponse.java        # { accessToken, refreshToken, user }
│   │   ├── FileUploadInitRequest.java
│   │   ├── FileUploadInitResponse.java
│   │   └── FileDto.java
│   ├── mapper/
│   │   ├── UserMapper.java          # MapStruct: User → UserDto
│   │   └── FileMapper.java          # MapStruct: FileMetadata → FileDto
│   ├── security/
│   │   ├── JwtUtil.java             # generate/validate access + refresh tokens
│   │   └── JwtAuthFilter.java       # OncePerRequestFilter
│   ├── scheduler/
│   │   └── OrphanFileCleanup.java   # @Scheduled — delete PENDING files older than 1h
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── StorageException.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_users.sql
│       └── V2__create_files.sql
├── src/test/java/com/storeit/
│   └── integration/                 # Testcontainers-based integration tests
├── docker-compose.yml
└── pom.xml
```

### PostgreSQL Schema
```sql
-- V1__create_users.sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMP DEFAULT NOW()
);

-- V2__create_files.sql
CREATE TABLE files (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    size         BIGINT NOT NULL,
    content_type VARCHAR(100),
    extension    VARCHAR(20),
    file_type    VARCHAR(20) NOT NULL,   -- document|image|video|audio|other
    status       VARCHAR(20) DEFAULT 'PENDING', -- PENDING|ACTIVE|FAILED
    shared_with  TEXT[],                 -- array of user emails
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_files_owner ON files(owner_id);
CREATE INDEX idx_files_name_fts ON files USING GIN(to_tsvector('english', filename));
```

### REST API Contract
#### Auth — `/api/auth`
| Method | Path          | Body / Notes                            | Response                         |
|--------|---------------|-----------------------------------------|----------------------------------|
| POST   | /register     | `{ email, fullName, password }`         | `{ accessToken, refreshToken, user }` |
| POST   | /login        | `{ email, password }`                   | `{ accessToken, refreshToken, user }` |
| POST   | /logout       | clears refresh token                    | 200 OK                           |
| POST   | /refresh      | `{ refreshToken }` → new access token   | `{ accessToken }`                |
| GET    | /me           | JWT header                              | `{ id, email, fullName, avatarUrl }` |

#### Files — `/api/files`
| Method | Path                | Query / Body                           | Response              |
|--------|---------------------|----------------------------------------|-----------------------|
| POST   | /upload-url         | `{ filename, contentType, size }`      | `{ fileId, presignedPutUrl }` |
| POST   | /:id/confirm        | —                                      | FileDto (ACTIVE)      |
| GET    | /                   | `?type=&search=&sort=&page=&size=`     | Page\<FileDto\>       |
| GET    | /:id                | —                                      | FileDto               |
| PATCH  | /:id/rename         | `{ name }`                             | FileDto               |
| DELETE | /:id                | — (also deletes from MinIO)            | 204 No Content        |
| GET    | /:id/download-url   | — (auth + ownership check)             | `{ presignedGetUrl }` |
| PATCH  | /:id/share          | `{ emails: [] }`                       | FileDto               |
| GET    | /space              | —                                      | `{ usedBytes, totalBytes }` |

### CORS Configuration (Critical)
Next.js (`:3000`) → Java (`:8080`) — must be explicitly allowed in `SecurityConfig`:
```java
config.setAllowedOrigins(List.of("http://localhost:3000"));
config.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

### JWT Strategy
- **Access token**: short-lived (15 min), sent as `Authorization: Bearer <token>` header
- **Refresh token**: long-lived (7 days), stored in httpOnly cookie (`storeit-refresh`)
- `POST /api/auth/refresh` issues a new access token silently (handled in `lib/api/client.ts`)
- Library: `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson`

### MinIO Docker Networking (Critical)
Presigned URLs are used **by the browser**, so MinIO must be reachable from the host:
```yaml
# docker-compose.yml
services:
  minio:
    image: minio/minio
    ports:
      - "9000:9000"   # S3 API — browser + backend both use this
      - "9001:9001"   # MinIO web console (admin UI)
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: storeit
      POSTGRES_USER: storeit
      POSTGRES_PASSWORD: storeit
```
S3Config must point to `http://localhost:9000` (not `http://minio:9000`) when
generating presigned URLs so the browser can resolve them.

### Orphan PENDING File Cleanup
```java
// OrphanFileCleanup.java
@Scheduled(cron = "0 0 * * * *") // every hour
public void cleanupOrphanedFiles() {
    Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
    List<FileMetadata> orphans = fileRepository
        .findByStatusAndCreatedAtBefore("PENDING", cutoff);
    orphans.forEach(f -> {
        storageService.deleteObject(f.getObjectKey()); // MinIO delete
        fileRepository.delete(f);
    });
}
```

### Frontend Adaptation (lib/api/ replaces lib/appwrite/)
```
lib/
├── api/
│   ├── client.ts      # base fetch wrapper: attaches JWT, handles 401→refresh→retry
│   ├── auth.api.ts    # register, login, logout, refresh, me
│   └── file.api.ts    # uploadUrl, confirmUpload, getFiles, rename, delete,
│                      # downloadUrl, share, getSpace
└── actions/           # keep structure — replace Appwrite calls with lib/api calls
    ├── user.actions.ts
    └── file.actions.ts
```
Auth form changes (done):
- `components/auth/AuthForm.tsx` — password field added; OTP step removed
- `components/OTPModal.tsx` — deleted (was dead/broken); `input-otp` dep removed
- Cookies `storeit_access_token` (15m) + `storeit-refresh` (7d, httpOnly) via
  `lib/auth/cookies.ts`; refresh→retry lives in `lib/actions/user.actions.ts`
- Env: `NEXT_PUBLIC_APPWRITE_*` still required for file storage; auth adds
  `NEXT_PUBLIC_API_URL=http://localhost:8080` (see `.env.example`)

### Implementation Phases
1. **Backend Foundation** — Spring Boot init, Docker Compose, entities, JWT auth endpoints
2. **File Metadata CRUD** — list (paginated), rename, delete, space; Flyway migrations
3. **MinIO Integration** — StorageService presigned PUT/GET, confirm endpoint, orphan cleanup
4. **Frontend Adaptation** — `lib/api/`, update actions, AuthForm, FileUploader, download
5. **Sharing + Polish** — `/share` endpoint, Testcontainers tests, full smoke test

### Env Variables (Backend — application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/storeit
    username: storeit
    password: storeit
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway manages schema
  flyway:
    enabled: true

app:
  jwt:
    secret: <256-bit-secret>
    access-token-expiry: 900      # 15 min (seconds)
    refresh-token-expiry: 604800  # 7 days (seconds)
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: storeit-files
    presigned-url-expiry: 900     # 15 min for PUT, 300 for GET
```
