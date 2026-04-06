# FileShare Platform — Implementation Plan

## Milestone 0 — Project Scaffold
- [x] Init monorepo: `/frontend`, `/backend`, `/worker`, `/docker`
- [ ] Create root `docker-compose.yml` with services: `frontend`, `backend`, `worker`, `postgres`, `rabbitmq`
- [ ] Add `.env.example` with all variables from §9
- [ ] Spring Boot parent POM with `backend` and `worker` as modules (shared `common` module for DTOs/enums)
- [x] Next.js app via `create-next-app` with TypeScript + Tailwind
- [ ] Verify all 5 containers start and reach healthy state

***

## Milestone 1 — Database & Shared Domain
- [ ] Write `V1__create_files_table.sql` Flyway migration (all columns from §5)
- [ ] Define `FileStatus` enum (`UPLOADING`, `PROCESSING`, `READY`, `EXPIRED`, `ERROR`)
- [ ] Define `FileType` enum (`VIDEO`, `IMAGE`, `OTHER`)
- [ ] Create `FileEntity` JPA entity + `FileRepository` (Spring Data JPA)
- [ ] Place enums + DTOs in `common` module so both `backend` and `worker` share them

***

## Milestone 2 — Upload Backend (tus Protocol)
- [ ] Implement `OPTIONS /api/upload` — return necessary tus discovery headers (`Tus-Resumable`, `Tus-Version`, `Tus-Extension`).
- [ ] Implement `POST /api/upload/init` — validate TTL, enforce `Content-Length` vs `MAX_FILE_SIZE_BYTES`, create DB row with `status=UPLOADING`, return upload URL.
- [ ] Implement `HEAD /api/upload/:id` — return `Upload-Offset` header from DB.
- [ ] Implement `PATCH /api/upload/:id` (or integrate a library like `tus-java-server`) — write chunk to `RandomAccessFile` at absolute offset, update offset in DB; on final chunk set `status=PROCESSING` and publish RabbitMQ message.
- [ ] Enforce max file size at `init` stage (reject with `413` before any bytes hit disk).
- [ ] IP rate limiting via `ConcurrentHashMap` + `@Scheduled` reset (or Bucket4j); return `429` + `Retry-After` on breach.
- [ ] Unit tests: protocol negotiation, chunk assembly, offset tracking, size rejection, rate limiting.

***

## Milestone 3 — File Metadata & Serving API
- [ ] `GET /api/files/:id` — return JSON (id, filename, mimeType, status, fileType, expiresAt, thumbnailUrl)
- [ ] `GET /api/files/:id/download` — stream original file with correct `Content-Disposition` + `Content-Type`; return `410 Gone` if expired
- [ ] `GET /thumbnails/:id` — serve thumbnail from disk
- [ ] `GET /hls/:id/index.m3u8` and `GET /hls/:id/:segment.ts` — serve HLS files from disk with correct MIME types (`application/vnd.apple.mpegurl`, `video/mp2t`)
- [ ] Global exception handler: expired → friendly JSON; missing → 404 with message

***

## Milestone 4 — Worker: Video Transcoding
- [ ] RabbitMQ queue config (`fileshare.processing` exchange + queue) in both `backend` (publisher) and `worker` (consumer)
- [ ] `@RabbitListener` consumes message with file UUID
- [ ] Detect `fileType` from MIME — route to video, image, or no-op path
- [ ] **Video path**: invoke FFmpeg via `ffmpeg-cli-wrapper` → output HLS (`-hls_time 6 -hls_playlist_type vod`) to `/app/uploads/<uuid>/hls/`; update `hls_path` in DB
- [ ] **Thumbnail extraction**: FFmpeg single-frame at 5s → `/app/uploads/<uuid>/thumb.jpg`; update `thumbnail_path`
- [ ] On FFmpeg non-zero exit: set `status=ERROR`, retain original file
- [ ] On success: set `status=READY`
- [ ] Worker integration test with a small real MP4

***

## Milestone 5 — Worker: Image Thumbnails & TTL Cleanup
- [ ] **Image path**: use `thumbnailator` to resize to max 400×400 preserving aspect ratio → save as `thumb.jpg`; set `status=READY`
- [ ] `@Scheduled(cron = "0 0 * * * *")` cleanup job using a `@Transactional` approach to prevent orphaned files:
  - Query all rows where `expires_at < NOW()` and `status != EXPIRED`
  - Delete files from disk *first*: original, HLS dir, thumbnail
  - *After* successful disk deletion, set `status=EXPIRED` in DB (prevents race conditions if I/O fails)
- [ ] Unit test: verify expired rows are found and correct paths are cleaned

***

## Milestone 6 — Frontend: Upload Page
- [ ] Drag & drop zone with `react-dropzone` (whole-page drop target)
- [ ] TTL selector dropdown (values from `DEFAULT_TTL_OPTIONS`)
- [ ] Wire `tus-js-client`: `POST` to `/api/upload/init`, then stream chunks via `PATCH`
- [ ] Upload progress bar (0–100%) driven by `tus-js-client` `onProgress` callback
- [ ] Pause / Resume buttons that call `tus.Upload.abort()` / `.start()`
- [ ] On upload complete → redirect to `/f/<uuid>`

***

## Milestone 7 — Frontend: File Page (`/f/[id]`)
- [ ] Fetch `GET /api/files/:id` on mount
- [ ] **Polling**: if `status === PROCESSING | UPLOADING`, re-fetch every 3 s via `setInterval`; cancel when `READY | EXPIRED | ERROR`
- [ ] **Video** (`fileType === VIDEO`): render `hls.js` player, set `poster=/thumbnails/:id`, load `/hls/:id/index.m3u8`
- [ ] **Image**: render `<img>` inline + download button
- [ ] **Other**: show filename, size, download button pointing to `/api/files/:id/download`
- [ ] **Expired / Error**: full-page friendly message, no broken elements
- [ ] Shareable URL display with one-click copy button
- [ ] QR code rendered with `qrcode.react` + "Download QR" button (`canvas.toDataURL`)
- [ ] **Server-Side Metadata**: Implement Next.js App Router's `generateMetadata` function to fetch file metadata server-side, ensuring `og:title`, `og:image`, and `og:url` are accurately exposed to social media scrapers before client hydration.

***

## Milestone 8 — Docker & DevOps Hardening
- [ ] `Dockerfile` for `backend` (multi-stage: Maven build → JRE 17 slim)
- [ ] `Dockerfile` for `worker` (same pattern; include `ffmpeg` + `imagemagick` in final image)
- [ ] `Dockerfile` for `frontend` (multi-stage: `npm build` → `node:alpine` serve)
- [ ] Named Docker volume `uploads_data` mounted into both `backend` and `worker` at `/app/uploads`
- [ ] Health checks for all services in `docker-compose.yml` (`depends_on: condition: service_healthy`)
- [ ] Nginx reverse proxy container: routes `/api/*` and `/hls/*` and `/thumbnails/*` → `backend`; `/` → `frontend`.
- [ ] **Nginx tus config**: Add `proxy_request_buffering off;` and `proxy_buffering off;` to the Nginx config for the upload routes to ensure the tus protocol's real-time chunked streaming works correctly.
- [ ] Validate full flow end-to-end in Docker Compose: upload → transcode → stream

***

## Milestone 9 — Polish & Edge Cases
- [ ] Handle disk-full IO exception: catch in `PATCH` handler, mark `ERROR`, delete partial chunks
- [ ] Out-of-order chunk test (write to absolute offset — should already pass from M2)
- [ ] `413` and `429` responses verified from frontend (show user-facing toast/banner)
- [ ] FFmpeg unsupported codec test → `ERROR` status → download fallback shown on file page
- [ ] Add `MAX_FILE_SIZE_BYTES` enforcement to Nginx (`client_max_body_size`)
- [ ] README: setup instructions, env var reference, local dev guide, Docker Compose quickstart

***


## Milestone 10 — Production Storage (Optional Upgrade)

> This milestone applies only when you want to deploy to a real server with actual users. For development, the `uploads_data` Docker volume is sufficient.

### Why you should do this

The local Docker volume has limitations in production:
- If the server is replaced, files are lost (no redundancy)
- You can't scale to multiple servers (both don't share the same disk)
- No automatic backup
- Disk fills up without automatic expansion

### Recommended Option: Cloudflare R2

**Why R2 for FileShare?** Your application is egress-heavy (users are constantly downloading videos and files). R2 has zero egress fees — AWS S3 charges ~$0.09/GB for every download.

| | **Cloudflare R2** | **AWS S3** |
|---|---|---|
| Storage | $0.015/GB | $0.023/GB |
| Egress | **$0 forever** | ~$0.09/GB |
| Free tier | 10 GB + 10M GETs/month (permanent) | 5 GB (first 12 months only) |
| S3-compatible API | Yes | Yes (original) |
| Built-in CDN | Yes | No (CloudFront separate) |

**Real-world calculation** (1,000 users/day, 50 MB average download = ~1.5 TB egress/month):
- AWS S3: ~$137/month
- Cloudflare R2: ~$1.35/month

### Implementation

Abstract behind a `StorageService` interface from the start:

```java
public interface StorageService {
    void upload(String key, InputStream data, long size, String contentType);
    InputStream download(String key);
    void delete(String key);
    String getPresignedUrl(String key, Duration expiry);
}
```

Two implementations — `LocalStorageService` (dev) and `R2StorageService` (production) — you only change config, not a single line of other code.

**R2 configuration (same AWS S3 SDK):**
```java
S3Client s3 = S3Client.builder()
    .region(Region.of("auto"))
    .endpointOverride(URI.create("https://<ACCOUNT_ID>.r2.cloudflarestorage.com"))
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create(R2_ACCESS_KEY, R2_SECRET_KEY)))
    .build();
```

**`.env` variables to add:**
```
STORAGE_TYPE=local          # or: r2
R2_ACCOUNT_ID=xxx
R2_ACCESS_KEY=xxx
R2_SECRET_KEY=xxx
R2_BUCKET=fileshare
```

### Alternatives

| Service | When to choose it |
|---|---|
| **Cloudflare R2** | File sharing, video streaming — no egress fees |
| **AWS S3** | Already in AWS ecosystem; want it on CV |
| **MinIO** (self-hosted) | Full control, own server, free |
| **Backblaze B2** | Budget option, $0.01/GB egress |

> **Note for CV:** AWS S3 API and R2 API are identical (S3-compatible). If you know R2, you know the S3 SDK. For the Skopje market in 2026, AWS S3 is most in-demand — but the code is the same.

### Checklist
- [ ] Create `StorageService` interface in the `common` module
- [ ] Implement `LocalStorageService` (reads/writes to `/app/uploads`) — use for dev
- [ ] Implement `R2StorageService` with AWS S3 SDK + R2 endpoint
- [ ] Add `STORAGE_TYPE` env variable to switch between implementations
- [ ] Update cleanup job (M5) to call `storageService.delete()` instead of direct `Files.delete()`
- [ ] Update serving endpoints (M3) to use presigned URLs for download instead of streaming


***


## Dependency Order

```text
M0 → M1 → M2 → M3 (can run parallel with M4)
              ↓
         M4 → M5
              ↓
    M6 → M7 (frontend can start after M3 API exists)
              ↓
         M8 → M9 → M10 (optional, only for production deployment)
```