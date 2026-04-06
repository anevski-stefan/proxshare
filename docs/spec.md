# FileShare Platform — Requirements Spec

## 1. Overview

A public, anonymous file-sharing and video-streaming web platform. Anyone can upload any file, get a shareable link and QR code, and share it with others. No user accounts or authentication required. Videos are streamed in-browser; all other files are downloaded directly.

---

## 2. Functional Requirements

### 2.1 Upload
- Any authenticated party (i.e. anyone with the URL) can upload files — no login required.
- Supported file types: **all** (video, audio, images, documents, archives, etc.).
- Max file size: configurable via environment variable (default: **2 GB per file**).
- Uploads use **chunked HTTP** with pause & resume support (`tus` protocol).
- An **upload progress bar** is shown in real time during upload.
- The uploader selects a **TTL (time-to-live)** at upload time from a predefined set of options (e.g. 1h / 6h / 24h / 72h / 7 days).
- On upload completion, the user receives:
  - A **unique shareable URL** (e.g. `https://domain.com/f/<uuid>`)
  - A **QR code** encoding that URL, rendered on the page and downloadable.

### 2.2 File Access (Recipient)
- Anyone with the shareable link can access the file page.
- **Videos** (MP4, MKV, WebM, MOV, etc.): played via an **in-browser HLS video player** with seeking support.
- **Images**: displayed inline with a preview thumbnail.
- **All other files** (audio, documents, archives): presented with a **direct download button**.
- If the TTL has expired, the page shows a clear "File no longer available" message (no broken links or 404s without context).

### 2.3 Video Processing
- Uploaded videos are transcoded asynchronously to **HLS format** (`.m3u8` manifest + `.ts` chunks) by a background worker.
- While processing, the file page shows a "Processing…" status with a polling mechanism (no page refresh needed).
- Once ready, the player loads automatically.
- A **thumbnail** is extracted from the video (at ~5s mark) and used as the player poster image and in any preview cards.

### 2.4 Image Previews
- On upload of an image, a **thumbnail** is generated server-side (e.g. 400×400 max, preserved aspect ratio).
- Thumbnail is shown on the file page and on any share cards (Open Graph meta tags).

### 2.5 TTL & Auto-Deletion
- A scheduled background job runs **every hour**.
- It queries the database for all files whose `expires_at` timestamp has passed.
- For each expired file: deletes physical files from disk (original + HLS chunks/thumbnails if applicable), then purges the database row.
- The file page reflects expiry immediately via the file's `status` field.

### 2.6 Abuse Prevention
- **Max file size**: enforced at the HTTP layer before any bytes are written to disk (check `Content-Length` / tus headers upfront).
- **Rate limiting per IP**: max N upload requests per hour per IP (N configurable via env var, default: 10). Returns `429 Too Many Requests` when exceeded.

---

## 3. Non-Functional Requirements

- **No authentication** — the platform is fully public and anonymous.
- **Resumable uploads** — interrupted uploads can be resumed without restarting from zero.
- **Async processing** — video transcoding never blocks the upload HTTP response.
- **Cloud-agnostic** — the entire stack runs via Docker Compose; no vendor-specific services. Can be deployed on any VPS or home server.
- **Stateless API** — the Spring Boot API holds no in-memory state; all state lives in Postgres.

---

## 4. Tech Stack

### Frontend
| Concern | Choice |
|---|---|
| Framework | Next.js (App Router) + TypeScript |
| Styling | Tailwind CSS |
| Chunked uploads | `tus-js-client` |
| Video playback | `hls.js` |
| Drag & drop zone | `react-dropzone` |
| QR code generation | `qrcode.react` |

### Backend (API)
| Concern | Choice |
|---|---|
| Runtime | Java 17+ |
| Framework | Spring Boot 3 (Web MVC) |
| ORM | Spring Data JPA + Hibernate |
| Upload endpoint | Custom `@RestController` with `RandomAccessFile` chunk assembly |
| Rate limiting | In-memory `ConcurrentHashMap` per IP (or Bucket4j) |
| Async dispatch | Spring AMQP → RabbitMQ |

### Worker (Background Processor)
| Concern | Choice |
|---|---|
| Runtime | Java 17+ / Spring Boot 3 |
| Queue consumer | `@RabbitListener` |
| Video transcoding | `ffmpeg-cli-wrapper` → HLS output |
| Thumbnail extraction | FFmpeg single-frame extract |
| Image thumbnail | ImageMagick or `thumbnailator` |
| TTL cleanup | `@Scheduled(cron = "0 0 * * * *")` |

### Data & Storage
| Concern | Choice |
|---|---|
| Database | PostgreSQL |
| Message broker | RabbitMQ |
| File storage | Local disk via Docker Volume (`/app/uploads`) |

### DevOps
| Concern | Choice |
|---|---|
| Containerisation | Docker + Docker Compose |
| Services | `frontend`, `backend`, `worker`, `postgres`, `rabbitmq` |
| Config | Environment variables (`.env` file) |

---

## 5. Data Model

### `files` table
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, used in shareable URL |
| `original_filename` | VARCHAR | Original name as uploaded |
| `mime_type` | VARCHAR | Detected MIME type |
| `size_bytes` | BIGINT | Total file size |
| `status` | ENUM | `UPLOADING`, `PROCESSING`, `READY`, `EXPIRED` |
| `file_type` | ENUM | `VIDEO`, `IMAGE`, `OTHER` |
| `storage_path` | VARCHAR | Absolute path to original file on disk |
| `hls_path` | VARCHAR | Path to `.m3u8` manifest (videos only) |
| `thumbnail_path` | VARCHAR | Path to generated thumbnail (video/image) |
| `uploader_ip` | VARCHAR | For rate limiting audit |
| `created_at` | TIMESTAMP | |
| `expires_at` | TIMESTAMP | `created_at + TTL chosen by uploader` |

---

## 6. API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/upload/init` | Initialise a tus upload session, return upload URL |
| `PATCH` | `/api/upload/:id` | Receive a chunk (tus protocol) |
| `HEAD` | `/api/upload/:id` | Query upload offset (tus resume) |
| `GET` | `/api/files/:id` | Get file metadata + status (polled by frontend) |
| `GET` | `/api/files/:id/download` | Stream original file as download |
| `GET` | `/hls/:id/index.m3u8` | Serve HLS manifest |
| `GET` | `/hls/:id/:segment.ts` | Serve HLS video segment |
| `GET` | `/thumbnails/:id` | Serve thumbnail image |

---

## 7. User Flows

### Upload Flow
1. User opens the homepage → drag & drop or click to select a file.
2. User picks a TTL from a dropdown.
3. Upload begins; progress bar shown. Upload can be paused/resumed.
4. On completion → file page opens showing the shareable URL, QR code, and file status.
5. If the file is a video or image, status polls every 3s until `READY`.

### View / Share Flow
1. Recipient opens the shareable URL.
2. If `status = PROCESSING` → "Processing…" spinner shown, polls until ready.
3. If `status = READY`:
   - **Video** → HLS player loads with poster thumbnail.
   - **Image** → Image shown inline.
   - **Other** → Download button shown.
4. If `status = EXPIRED` → "This file is no longer available" message shown.

---

## 8. Edge Cases

| Scenario | Handling |
|---|---|
| Upload interrupted mid-chunk | tus client resumes from last confirmed offset on retry |
| File exceeds size limit | Rejected before first byte written; `413 Payload Too Large` |
| IP exceeds rate limit | `429 Too Many Requests` with `Retry-After` header |
| FFmpeg transcoding fails | Status set to `ERROR`; file page shows failure message; original file retained for direct download fallback |
| Recipient opens link after expiry | `status = EXPIRED` served from DB; page shows friendly expiry message |
| Two chunks arrive out of order | `RandomAccessFile` writes to absolute byte offset; order is irrelevant |
| Disk full during upload | IO exception caught, upload marked `ERROR`, temp chunks cleaned up |
| Unsupported video codec | FFmpeg returns non-zero exit; handled as transcoding failure (see above) |

---

## 9. Environment Variables (`.env`)

```
MAX_FILE_SIZE_BYTES=2147483648
RATE_LIMIT_UPLOADS_PER_HOUR=10
DEFAULT_TTL_OPTIONS=1h,6h,24h,72h,7d
UPLOAD_DIR=/app/uploads
POSTGRES_URL=jdbc:postgresql://postgres:5432/fileshare
RABBITMQ_HOST=rabbitmq
```