<div align="center">
  <br />
    <img src="public/readme/hero.png" alt="StoreIt Project Banner" />
  <br />

  <div>
     <img src="https://img.shields.io/badge/-Next_JS-black?style=for-the-badge&logoColor=white&logo=nextdotjs&color=000000" alt="nextdotjs" />
    <img src="https://img.shields.io/badge/-React-black?style=for-the-badge&logoColor=white&logo=react&color=61DAFB" alt="react" />
    <img src="https://img.shields.io/badge/-TypeScript-black?style=for-the-badge&logoColor=white&logo=typescript&color=3178C6" alt="typescript" />
    <img src="https://img.shields.io/badge/-Tailwind_CSS-black?style=for-the-badge&logoColor=white&logo=tailwindcss&color=06B6D4" alt="tailwindcss" />
    <img src="https://img.shields.io/badge/-Spring_Boot-black?style=for-the-badge&logoColor=white&logo=springboot&color=6DB33F" alt="springboot" />
    <img src="https://img.shields.io/badge/-PostgreSQL-black?style=for-the-badge&logoColor=white&logo=postgresql&color=4169E1" alt="postgresql" />
    <img src="https://img.shields.io/badge/-MinIO-black?style=for-the-badge&logoColor=white&logo=minio&color=C72E49" alt="minio" />
    <img src="https://img.shields.io/badge/-Appwrite-black?style=for-the-badge&logoColor=white&logo=appwrite&color=FD366E" alt="appwrite" />
  </div>

<h3 align="center">StoreIt — Storage & File Sharing Platform</h3>

  <div align="center">
    A Google-Drive-style personal cloud: upload, organize, search, share, and manage files with a hybrid storage backend.
  </div>
</div>

## 📋 Table of Contents

1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Features](#features)
5. [Project Structure](#project-structure)
6. [Quick Start](#quick-start)
7. [Environment Variables](#environment-variables)
8. [API Overview](#api-overview)
9. [Testing](#testing)
10. [Commands](#commands)

## 🤖 Introduction

StoreIt is a full-stack storage management and file sharing platform. Users sign up, upload files (documents, images, video, audio), browse them by type, rename/share/download/delete, and track usage from a dashboard with charts.

Unlike a pure Appwrite demo, this codebase is a **hybrid system**:

- **Frontend:** Next.js 15 (App Router) + React 19 + TypeScript + TailwindCSS + shadcn/ui
- **Auth + file metadata API:** custom **Java Spring Boot** backend (JWT)
- **File bytes:** either **Appwrite Cloud Storage** (default) or local **MinIO** (S3-compatible), chosen per upload in the UI

The backend never streams file bytes — it issues **presigned URLs** so the browser talks directly to MinIO. Appwrite uploads still go through the Next.js server actions using the Appwrite Node SDK.

## 🏗 Architecture

```
Browser (Next.js :3000)
   │
   ├─ Auth ──────────────► Java Spring Boot (:8080)
   │                        ├─ JWT access (15m) + refresh (7d, httpOnly cookie)
   │                        ├─ PostgreSQL 16 (users, files metadata)
   │                        └─ Presigned PUT/GET URLs ──► MinIO (:9000)
   │
   └─ Appwrite uploads ──► Appwrite Cloud (Database + Storage)
        (server actions; default backend)
```

**Upload flow (MinIO):** browser asks backend for upload URL → backend creates a `PENDING` row + presigned PUT → browser uploads straight to MinIO → `confirm` marks the file `ACTIVE` after verification.

**Upload flow (Appwrite):** browser → Next.js server action → Appwrite Storage bucket (50MB max).

Orphaned `PENDING` MinIO records are cleaned up by a scheduled task (older than 1 hour).

## ⚙️ Tech Stack

### Frontend
- **Next.js 15.0.2** (App Router, Turbopack dev, Server Actions)
- **React 19 RC** + **TypeScript**
- **TailwindCSS** + **shadcn/ui** (Radix primitives)
- **react-hook-form** + **zod**, **react-dropzone**, **recharts**, **use-debounce**
- **node-appwrite** (file storage actions only — not auth)
- **Vitest** + Testing Library

### Backend (`backend/`)
- **Java 21** + **Spring Boot 3.3.5**
- **Spring Security 6** + **jjwt** (JWT filter)
- **Spring Data JPA** + **Hibernate** (`ddl-auto: validate`)
- **PostgreSQL 16** + **Flyway** migrations
- **AWS SDK for Java v2** → MinIO (S3 API), presigned URLs
- **Lombok** + **MapStruct**, **springdoc-openapi** (Swagger UI)
- **Testcontainers** for integration tests (excluded by default)

### Infrastructure
- **Appwrite Cloud** — default file storage + users/files tables
- **MinIO** (via `pgsty/silo` compatible image) — local S3 object storage
- **Docker/Podman Compose** — PostgreSQL + MinIO for local backend dev

## 🔋 Features

### Authentication
- Register / login / logout against the **Java backend** (email + password, BCrypt)
- Access token (15 min) attached as `Authorization: Bearer …`
- Refresh token (7 days) in httpOnly cookie `storeit-refresh`; `apiFetch` retries once on 401 after refresh
- Auth guard on the root layout (`getCurrentUser` or redirect to `/sign-in`)

### File management
- **Drag-and-drop upload** (react-dropzone), max **50MB** per file
- **Backend toggle:** each upload can target **Appwrite** or **MinIO** (segmented control in the uploader)
- Browse by type: `/documents`, `/images`, `/media` (video+audio), `/others`
- **Rename**, **share** (by email), **download**, **delete**, **details** modal
- Thumbnails + type-specific icons; animated GIFs render unoptimized
- **Search** (debounced, `?query=`) and **sort** by date, name, or size (`?sort=`)
- Cards show an **Appwrite** / **MinIO** **storage badge**

### Dashboard
- Usage chart(s) per storage backend (Appwrite vs MinIO)
- Total/consumed storage summary cards by type
- Recent files list (both backends, with badges)
- 2GB quota helpers for Appwrite usage display

### Platform
- Shared-with-me files appear in listings (owner **or** shared email)
- Server Actions body size limit **100MB**
- Responsive layout: sidebar + mobile navigation + header
- Image remote patterns for Appwrite, CDN previews, and `localhost:9000` MinIO

## 📁 Project Structure

```
.
├── app/
│   ├── (auth)/sign-in, sign-up     # AuthForm (email + password)
│   ├── (root)/page.tsx             # Dashboard
│   ├── (root)/[type]/page.tsx      # documents | images | media | others
│   └── (root)/layout.tsx           # Auth guard, sidebar, uploader, toaster
├── components/
│   ├── auth/                       # AuthForm
│   ├── dashboard/                  # Chart, StorageCard, RecentFiles, BackendSwitcher, …
│   ├── files/                      # FileUploader, Card, ActionDropdown, StorageBadge, …
│   ├── layout/                     # Sidebar, Header, MobileNavigation
│   ├── search/                     # Search, Sort
│   └── ui/                         # shadcn/ui primitives
├── lib/
│   ├── actions/                    # Server actions: user, file (Appwrite + MinIO)
│   ├── api/                        # apiFetch, auth.api, file.api (Java backend)
│   ├── auth/                       # cookies, session helpers
│   ├── appwrite/                   # createAdminClient, config
│   ├── files/                      # unified mapping, minio-upload
│   └── utils.ts                    # file type, icons, sizes, URLs, usage summary
├── constants/index.ts              # nav, actions, sort types, MAX_FILE_SIZE
├── types/index.d.ts                # FileType, UnifiedFile, props interfaces
├── __tests__/                      # Vitest: unified, Card, Chart, StorageBadge, FileUploader
├── backend/                        # Spring Boot API (see backend/README.md)
│   ├── docker-compose.yml          # PostgreSQL 16 + MinIO (Silo)
│   ├── mvnw                        # Maven wrapper
│   └── src/main/java/com/storeit/  # config, controller, service, security, …
└── public/readme/                  # README assets
```

## 🤸 Quick Start

### Prerequisites

- **Git**, **Node.js** (v24 recommended on this machine), **npm** (use system `/usr/bin/npm`, not a stale local npm)
- For the backend: **JDK 21**, Docker or Podman (compose)

### Frontend

```bash
# From repo root
/usr/bin/npm install

# Create .env.local from the example
cp .env.example .env.local
# Fill in Appwrite credentials + NEXT_PUBLIC_API_URL

/usr/bin/npm run dev
```

Open [http://localhost:3000](http://localhost:3000). You will be redirected to `/sign-in`.

After changing `.env.local` or `next.config.ts`, clear the build cache:

```bash
rm -rf .next
```

### Backend (auth + MinIO)

```bash
cd backend

# Start PostgreSQL + MinIO
docker compose up -d
# or: podman-compose up -d

# Build and run with bundled JDK 21 (system Java must be 21 for Lombok)
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw spring-boot:run
# or package a jar:
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw clean package
JAVA_HOME=$PWD/.tools/jdk-21 java -jar target/storeit-backend-0.0.1-SNAPSHOT.jar
```

| Service | URL | Notes |
|---------|-----|--------|
| Frontend | `http://localhost:3000` | Next.js |
| Backend API | `http://localhost:8080` | REST |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | springdoc |
| MinIO S3 | `http://localhost:9000` | Browser presigned PUT/GET |
| MinIO console | `http://localhost:9001` | `minioadmin` / `minioadmin` |
| PostgreSQL | `localhost:5432` | db/user/pass: `storeit` |

More detail: [`backend/README.md`](backend/README.md).

## 🔐 Environment Variables

### Frontend — `.env.local` (see `.env.example`)

```env
NEXT_PUBLIC_APPWRITE_ENDPOINT="https://cloud.appwrite.io/v1"
NEXT_PUBLIC_APPWRITE_PROJECT=""
NEXT_PUBLIC_APPWRITE_DATABASE=""
NEXT_PUBLIC_APPWRITE_USERS_COLLECTION=""
NEXT_PUBLIC_APPWRITE_FILES_COLLECTION=""
NEXT_PUBLIC_APPWRITE_BUCKET=""
NEXT_APPWRITE_KEY=""
NEXT_PUBLIC_API_URL="http://localhost:8080"
```

Appwrite IDs come from your Appwrite project (database, users/files tables, storage bucket, API key with databases/storage scopes). `NEXT_PUBLIC_API_URL` must match the Spring Boot origin (CORS allows `http://localhost:3000`).

### Backend — `application.yml` defaults

| Key | Default |
|-----|---------|
| Datasource | `jdbc:postgresql://localhost:5432/storeit` (`storeit`/`storeit`) |
| JWT access / refresh | 900s / 604800s |
| MinIO endpoint | `http://localhost:9000` |
| MinIO bucket | `storeit-files` |
| Presigned PUT / GET | 900s / 300s |
| Server port | `8080` |

Override via Spring env vars (e.g. `SPRING_DATASOURCE_URL=...`). Schema is Flyway-managed — add migrations under `backend/src/main/resources/db/migration/`, never hand-edit tables.

## 🔌 API Overview

Full contract: Swagger UI. Summary:

**Auth — `/api/auth`**
`POST /register`, `POST /login`, `POST /logout`, `POST /refresh`, `GET /me`

**Files — `/api/files`**

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/upload-url` | PENDING record + presigned PUT |
| POST | `/{id}/confirm` | Verify object, mark ACTIVE |
| GET | `/` | List (`type`, `search`, `sort`, `page`, `size`) — owned **and** shared |
| GET | `/{id}` | Single file |
| PATCH | `/{id}/rename` | Rename |
| DELETE | `/{id}` | Delete MinIO object + DB row |
| GET | `/{id}/download-url` | Presigned GET (auth + ownership) |
| PATCH | `/{id}/share` | Set shared emails |
| GET | `/space` | `{ usedBytes, totalBytes }` |

`FileDto` includes presigned `url`, `ownerName`, `ownerEmail`, and `storageBackend: "minio"` so the UI can render MinIO files with the same components as Appwrite files. `type` accepts comma-separated lists (e.g. `video,audio`).

## 🧪 Testing

### Frontend (Vitest)

```bash
/usr/bin/npm test          # single run
/usr/bin/npm run test:watch
```

Tests live under `__tests__/` (unified file mapping, Card, Chart, StorageBadge, FileUploader).

### Backend

```bash
# Unit tests (Testcontainers excluded)
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw test

# Integration tests (needs Docker)
JAVA_HOME=$PWD/.tools/jdk-21 ./mvnw test -Dgroups=integration -DexcludedGroups=
```

## 🚀 Commands

| Where | Command | Action |
|-------|---------|--------|
| Root | `/usr/bin/npm run dev` | Next.js dev (Turbopack) |
| Root | `/usr/bin/npm run build` | Production build |
| Root | `/usr/bin/npm start` | Serve production build |
| Root | `/usr/bin/npm run lint` | ESLint |
| Root | `/usr/bin/npm test` | Vitest |
| `backend/` | `./mvnw spring-boot:run` | Run API (dev) |
| `backend/` | `./mvnw clean package` | Build + unit tests + jar |
| `backend/` | `docker compose up -d` | PostgreSQL + MinIO |

## Smoke test (manual)

1. Start backend + MinIO + frontend; open `/sign-up` → register → land on `/`.
2. Upload a file with **Appwrite** and again with **MinIO**; confirm badges and dashboard charts.
3. Search, sort, open each type page; rename / share / download / delete.
4. Sign out, sign in again (JWT refresh path), verify session persists across reloads.
