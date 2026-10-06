# AGENTS.md — StoreIt (Storage Management & File Sharing Platform)

## What this is
Next.js 15 (App Router) + React 19 + TypeScript + TailwindCSS + shadcn/ui app backed by
Appwrite Cloud (Auth via Email OTP, Database, Storage). Google-Drive-like: upload, browse
by type, rename/share/download/delete, dashboard quota, search + sort.

## Stack
- Next.js 15.0.2, React 19 RC (`19.0.0-rc-02c0e824-20241028`), `node-appwrite@14.1.0`
  (exports only `Databases` — no `TablesDB`; keep using `listDocuments/createDocument/...`).
- TailwindCSS + shadcn/ui (Radix), react-hook-form + zod, recharts, use-debounce.
- Server Actions (`"use server"`) for all Appwrite calls; `experimental.serverActions.bodySizeLimit: 100MB`.

## Routes & key files
- `app/(auth)/sign-in`, `sign-up` → `components/AuthForm.tsx` + `components/OTPModal.tsx`
- `app/(root)/layout.tsx` — auth guard (`getCurrentUser` or redirect), `Sidebar`,
  `MobileNavigation`, `Header`, `FileUploader`, `Toaster`; `dynamic = force-dynamic`.
- `app/(root)/page.tsx` — Dashboard (`Chart`, usage summary, recent files).
- `app/(root)/[type]/page.tsx` — type pages (`documents|images|media|others`), `Sort`, `Card` grid.
- `components/` — `FileUploader` (react-dropzone, MAX_FILE_SIZE 50MB), `Card`,
  `ActionDropdown` + `ActionsModalContent`, `Chart`, `Search`, `Sort`, `Thumbnail`.
- `lib/appwrite/index.ts` — `createAdminClient` (API key) vs `createSessionClient`
  (cookie `appwrite-session`). `lib/appwrite/config.ts` reads env below.
- `lib/actions/user.actions.ts` — `sendEmailOTP`, `createAccount`, `verifySecret`,
  `getCurrentUser`, `signInUser`, `signOutUser`.
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
