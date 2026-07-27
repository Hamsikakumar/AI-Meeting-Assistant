# AI Meeting Assistant

An AI-powered meeting assistant that transcribes recordings, generates summaries, extracts action items, and more.

**Tech Stack:** React (frontend) + Spring Boot (backend) + PostgreSQL (database) + Whisper/LLM (AI processing, upcoming)

---

## Project Status

- [x] **Phase 1 — Authentication** (complete)
- [x] **Phase 2 — File Upload** (complete)
- [x] **Phase 3 — Transcription Pipeline** (complete)
- [ ] Phase 4 — LLM Processing (summaries, action items, deadlines)
- [ ] Phase 5 — Core UI (meeting history, search)
- [ ] Phase 6 — Team & Sharing Features
- [ ] Phase 7 — Polish & Deployment

---

## Prerequisites

Make sure these are installed before running the project:

- **Java 17+** (JDK)
- **Node.js** (for React/Vite)
- **Docker Desktop** (for PostgreSQL)
- **VS Code** with Java Extension Pack + Spring Boot Extension Pack

---

## How to Run This Project

### 1. Start PostgreSQL (Docker)

From the project root:

```bash
docker compose up -d
```

To verify it's running:

```bash
docker ps
```

You should see a container using `postgres:16` with port `5432` mapped.

**If you ever see "connection refused" or "database does not exist" errors**, it usually means the container isn't running or the volume needs to be reset:

```bash
docker compose down -v
docker compose up -d
```

### 2. Start the Backend (Spring Boot)

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for the log line: `Started BackendApplication in X seconds`

The backend runs on **http://localhost:8080**

**Known Windows quirk:** Java may report the timezone as `Asia/Calcutta` (a legacy name) which Postgres doesn't recognize as `Asia/Kolkata`. This is already fixed in `application.properties` via the JDBC URL parameter `?options=-c%20TimeZone=Asia/Kolkata` — you shouldn't need to do anything extra, but if timezone errors reappear, that's the cause.

### 3. Start the Frontend (React + Vite)

Open a **new terminal** (keep the backend running):

```bash
cd frontend
npm run dev
```

The frontend runs on **http://localhost:5173**

Open that URL in your browser — you should land on the Login page.

---

## What Was Built in Phase 1 — Authentication

### Backend (`backend/src/main/java/com/meetingassistent/backend/`)

| File | Purpose |
|---|---|
| `model/User.java` | User entity (id, name, email, password, role, createdAt) |
| `model/Role.java` | Enum: USER / ADMIN |
| `repository/UserRepository.java` | JPA repository for User, includes `findByEmail` |
| `dto/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java` | Request/response shapes for auth endpoints |
| `config/PasswordConfig.java` | BCrypt password encoder bean |
| `security/JwtUtil.java` | Generates and validates JWT tokens |
| `security/JwtAuthFilter.java` | Intercepts requests, validates JWT, sets authenticated user in context |
| `config/SecurityConfig.java` | Configures which routes are public vs protected, wires up the JWT filter, CORS settings |
| `service/AuthService.java` | Business logic for register/login |
| `controller/AuthController.java` | Exposes `POST /api/auth/register` and `POST /api/auth/login` |

**How auth works:**
1. Register: name/email/password → password hashed with BCrypt → user saved to DB → JWT issued
2. Login: email/password → password verified against hash → JWT issued
3. Protected routes: any request must include `Authorization: Bearer <token>` header; `JwtAuthFilter` validates it and identifies the user for that request
4. Tokens expire after 24 hours (configurable in `JwtUtil.java`)

### Frontend (`frontend/src/`)

| File | Purpose |
|---|---|
| `api/auth.js` | Functions that call the backend's register/login endpoints |
| `context/AuthContext.jsx` | Global state for "is a user logged in", stores token + user info in `localStorage` |
| `pages/Login.jsx` | Login form |
| `pages/Register.jsx` | Registration form |
| `pages/Dashboard.jsx` | Placeholder protected page shown after login |
| `components/ProtectedRoute.jsx` | Redirects to `/login` if no authenticated user |
| `App.jsx` | Sets up routing: `/login`, `/register`, `/dashboard` (protected) |

**Verified working:**
- Register → auto-login → redirected to dashboard
- Logout → redirected to login
- Manually visiting `/dashboard` while logged out → redirected to login (route protection actually works, not just hidden UI)
- Re-login with existing credentials works
- Refreshing the page while logged in keeps you logged in (token persists via `localStorage`)

---

## What Was Built in Phase 2 — File Upload

### Backend (new files)

| File | Purpose |
|---|---|
| `model/Meeting.java` | Meeting entity (id, user, originalFilename, storedFilename, status, transcript, createdAt) |
| `model/MeetingStatus.java` | Enum: UPLOADED / PROCESSING / COMPLETED / FAILED |
| `repository/MeetingRepository.java` | JPA repository, `findByUserOrderByCreatedAtDesc` |
| `dto/MeetingResponse.java` | Response shape returned to frontend |
| `service/FileStorageService.java` | Saves uploaded files to disk with a UUID-based unique filename (avoids collisions) |
| `service/MeetingService.java` | Business logic: upload handling, fetching meetings for a user |
| `controller/MeetingController.java` | Exposes `POST /api/meetings/upload` and `GET /api/meetings` (both protected) |

**Config added:** `file.upload-dir=uploads` in `application.properties` — files are stored in `backend/uploads/` (git-ignored, never committed).

**How it works:** `@AuthenticationPrincipal User user` in the controller automatically injects the logged-in user from the JWT (via the `JwtAuthFilter` from Phase 1) — no manual token parsing needed in the controller itself.

### Frontend (new files)

| File | Purpose |
|---|---|
| `api/meetings.js` | Functions to call upload/get-meetings endpoints |
| `pages/Dashboard.jsx` (updated) | File picker + upload button + list of meetings |

**Verified working:** file uploaded through the browser UI appears in the meetings list, backed by the real database.

---

## What Was Built in Phase 3 — Transcription Pipeline

### The core problem this phase solves

Transcription takes real time (seconds to minutes) and can't happen inside a normal request/response cycle without blocking the user. This phase introduces **asynchronous background processing**.

### Backend (new/changed files)

| File | Purpose |
|---|---|
| `BackendApplication.java` (updated) | Added `@EnableAsync` to allow background-thread methods |
| `model/Meeting.java` (updated) | Added `transcript` field (`TEXT` column, for long content) |
| `service/TranscriptionService.java` | Calls the Whisper API asynchronously, updates meeting status and transcript |
| `service/MeetingService.java` (updated) | Upload now triggers `transcriptionService.transcribeMeeting(...)` right after saving |
| `dto/MeetingResponse.java` (updated) | Now includes `transcript` field |

**How the async flow works:**
1. `POST /api/meetings/upload` saves the file and DB row, returns immediately with status `UPLOADED`
2. `TranscriptionService.transcribeMeeting(...)` (marked `@Async`) runs on a background thread
3. It sets status to `PROCESSING`, sends the audio file to the transcription API, waits for the result
4. On success: saves the transcript, sets status to `COMPLETED`
5. On failure: sets status to `FAILED`, logs the error to the console

**Important pivot — OpenAI vs Groq:** originally built against OpenAI's Whisper API, but this requires a funded billing account (no free tier). Switched to **Groq's API** instead, which is OpenAI-compatible (same request/response shape) and offers a **free tier** hosting `whisper-large-v3`. Only three lines needed to change: the API key, the base URL (`https://api.groq.com/openai/v1`), and the model name (`whisper-large-v3`). Worth remembering this pattern — many providers now mimic OpenAI's API shape, making swaps like this easy.

**Config:**
```properties
groq.api.key=<your key>
```

### Frontend (updated files)

| File | Purpose |
|---|---|
| `pages/Dashboard.jsx` (updated) | Polls `GET /api/meetings` every 5 seconds to catch status changes automatically; displays color-coded status (green/orange/red/gray); click a completed meeting to expand and view its full transcript inline |

**Verified working:** uploaded a real audio file, watched status move UPLOADED → PROCESSING → COMPLETED automatically without refreshing, and confirmed the transcript was accurate to the actual spoken audio content.

---

## Environment / Config Notes

- **Database name:** `meeting_assistant`
- **Database credentials (local dev only):** `postgres` / `postgres`
- **JWT secret:** stored in `backend/src/main/resources/application.properties` under `jwt.secret` — generated via `openssl rand -base64 32`. **Never commit this to a public repo in a real project** — should eventually move to an environment variable.
- **CORS:** currently only allows requests from `http://localhost:5173` (configured in `SecurityConfig.java`) — update this if the frontend port changes or when deploying.

---

## Troubleshooting Log (Phase 1)

Issues actually encountered and fixed while building this phase, in case they resurface:

1. **`database "meeting_assistant" does not exist`** — caused by an old Docker volume from before `POSTGRES_DB` was set correctly. Fixed with `docker compose down -v && docker compose up -d`.
2. **Orphaned container holding port 5432** — happened after renaming the project folder (Docker Compose names containers after the folder name). Fixed by manually stopping/removing the old container.
3. **`invalid value for parameter "TimeZone": "Asia/Calcutta"`** — Windows + Java reports an outdated timezone alias that Postgres 16 doesn't recognize. Fixed via the JDBC URL parameter (see above).
4. **`cannot find symbol: UsernamePasswordAuthenticationFilter`** — wrong import path. Correct package is `org.springframework.security.web.authentication`, not `org.springframework.security.authentication`.
5. **Got 403 instead of 401 for unauthenticated requests** — expected behavior for this custom JWT setup without a configured `AuthenticationEntryPoint`; not a bug.
6. **Thunder Client file upload paywall** — Thunder Client's file-upload feature is now paid-only. Switched to `curl.exe` (Windows' built-in real curl, called explicitly to bypass PowerShell's `curl` alias which points to `Invoke-WebRequest` instead) for testing multipart uploads from the terminal.
7. **PowerShell `-Form` parameter not found** — caused by using old Windows PowerShell 5.1, which lacks `Invoke-RestMethod -Form` (added in PowerShell 7+). Worked around this using `curl.exe` instead of upgrading PowerShell.
8. **OpenAI `429 Too Many Requests` on first-ever request** — misleading error; actually meant no billing/credits on the account, not literal rate limiting. Since a funded OpenAI account wasn't available, switched to Groq's free-tier Whisper API instead (see Phase 3 section above).
9. **Stray/duplicated code after AI-assisted edits** — a couple of file edits accidentally left duplicated class bodies or fields placed outside the class braces, causing confusing "class expected" compiler errors. Lesson: after any AI-suggested edit, briefly re-view the whole file before running, especially for short files where a full replace is safer than a partial edit.

---

## Next Steps (Phase 4 Preview)

- Send completed transcripts to an LLM (Groq/Llama or similar) to generate:
  - Meeting summary
  - Action items list
  - Key deadlines mentioned
- Store these structured results alongside the transcript
- Display summary/action items/deadlines in the Dashboard UI